package com.example.product_api.service;

import com.example.product_api.model.*;
import com.example.product_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {
    private static final double DELIVERY_FEE = 30;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final AddressRepository addresses;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final NotificationRepository notifications;
    private final DeliveryRepository deliveries;

    public OrderService(CustomerOrderRepository orders, OrderItemRepository orderItems,
                        CartItemRepository cartItems, ProductRepository products,
                        AddressRepository addresses, UserRepository users, PaymentRepository payments,
                        NotificationRepository notifications, DeliveryRepository deliveries) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.cartItems = cartItems;
        this.products = products;
        this.addresses = addresses;
        this.users = users;
        this.payments = payments;
        this.notifications = notifications;
        this.deliveries = deliveries;
    }

    @Transactional
    public Map<String, Object> place(Long userId, CheckoutRequest request) {
        if (request == null || request.addressId() == null || request.paymentMethod() == null
                || !Set.of("CASH_ON_DELIVERY", "ONLINE", "UPI", "CARD").contains(request.paymentMethod())) {
            throw new IllegalArgumentException("Choose a delivery address and supported payment method");
        }
        Address address = addresses.findByIdAndUser_Id(request.addressId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Select a valid delivery address"));
        List<CartItem> cart = cartItems.findByUser_IdForUpdate(userId);
        if (cart.isEmpty()) throw new IllegalArgumentException("Your cart is empty");

        Map<Long, Product> lockedProducts = new HashMap<>();
        cart.stream().map(item -> item.getProduct().getId()).distinct().sorted().forEach(id ->
                lockedProducts.put(id, products.findByIdForUpdate(id)
                        .orElseThrow(() -> new IllegalArgumentException("A product is no longer available"))));

        double subtotal = 0;
        for (CartItem item : cart) {
            Product product = lockedProducts.get(item.getProduct().getId());
            if (!"ACTIVE".equals(product.getStatus())) throw new IllegalArgumentException(product.getName() + " is no longer available");
            if (item.getQuantity() < 1 || item.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Only " + product.getStockQuantity() + " units of " + product.getName() + " are currently available.");
            }
            subtotal += product.getPrice() * item.getQuantity();
        }
        double deliveryFee = subtotal >= 500 ? 0 : DELIVERY_FEE;
        User user = users.findById(userId).orElseThrow();
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setAddress(address);
        order.setDeliveryAddress(formatAddress(address));
        order.setItemSubtotal(round(subtotal));
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(round(subtotal + deliveryFee));
        order.setPaymentMethod(request.paymentMethod());
        orders.saveAndFlush(order);

        for (CartItem item : cart) {
            Product product = lockedProducts.get(item.getProduct().getId());
            double lineTotal = round(product.getPrice() * item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            products.save(product);
            OrderItem line = new OrderItem();
            line.setOrder(order);
            line.setProduct(product);
            line.setProductName(product.getName());
            line.setCapacity(product.getCapacity());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(product.getPrice());
            line.setSubtotal(lineTotal);
            orderItems.save(line);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setUser(user);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(request.paymentMethod());
        payments.save(payment);
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        deliveries.save(delivery);
        notify(user, order, "Your order #" + order.getId() + " has been placed.");
        cartItems.deleteAll(cart);
        return view(order);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForCustomer(Long userId) {
        return orders.findByUser_IdOrderByOrderDateDesc(userId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getForCustomer(Long orderId, Long userId) {
        return orders.findByIdAndUser_Id(orderId, userId).map(this::view)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional
    public Map<String, Object> cancel(Long orderId, Long userId) {
        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!"PLACED".equals(order.getOrderStatus())) throw new IllegalArgumentException("This order can no longer be cancelled");
        order.setOrderStatus("CANCELLED");
        for (OrderItem line : orderItems.findByOrder_Id(orderId)) {
            Product product = products.findByIdForUpdate(line.getProduct().getId()).orElseThrow();
            product.setStockQuantity(product.getStockQuantity() + line.getQuantity());
            products.save(product);
        }
        deliveries.findByOrder_Id(orderId).ifPresent(delivery -> {
            delivery.setDeliveryStatus("CANCELLED");
            deliveries.save(delivery);
        });
        CustomerOrder saved = orders.save(order);
        notify(order.getUser(), order, "Your order #" + order.getId() + " has been cancelled.");
        return view(saved);
    }

    @Transactional
    public Map<String, Object> reorder(Long orderId, Long userId) {
        CustomerOrder order = orders.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        for (OrderItem line : orderItems.findByOrder_Id(order.getId())) {
            Product product = products.findById(line.getProduct().getId())
                    .filter(item -> "ACTIVE".equals(item.getStatus()))
                    .orElseThrow(() -> new IllegalArgumentException(line.getProductName() + " is no longer available"));
            int targetQuantity = cartItems.findByUser_IdAndProduct_Id(userId, product.getId())
                    .map(item -> item.getQuantity() + line.getQuantity()).orElse(line.getQuantity());
            if (targetQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Only " + product.getStockQuantity() + " units of " + product.getName() + " are currently available.");
            }
            CartItem item = cartItems.findByUser_IdAndProduct_Id(userId, product.getId()).orElseGet(() -> {
                CartItem created = new CartItem();
                created.setUser(order.getUser());
                created.setProduct(product);
                return created;
            });
            item.setQuantity(targetQuantity);
            cartItems.save(item);
        }
        return Map.of("message", "Items added to your cart");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return orders.findAll().stream().map(this::view).toList();
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status) {
        if (!Set.of("CONFIRMED", "PROCESSING", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Invalid order status");
        }
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Set<String> allowed = switch (order.getOrderStatus()) {
            case "PLACED" -> Set.of("CONFIRMED", "CANCELLED");
            case "CONFIRMED" -> Set.of("PROCESSING", "CANCELLED");
            case "PROCESSING" -> Set.of("PACKED", "CANCELLED");
            case "PACKED" -> Set.of("OUT_FOR_DELIVERY", "CANCELLED");
            case "OUT_FOR_DELIVERY" -> Set.of("DELIVERED");
            default -> Set.of();
        };
        if (!allowed.contains(status)) throw new IllegalArgumentException("Order cannot move from " + order.getOrderStatus() + " to " + status);
        if ("CANCELLED".equals(status)) {
            for (OrderItem line : orderItems.findByOrder_Id(id)) {
                Product product = products.findByIdForUpdate(line.getProduct().getId()).orElseThrow();
                product.setStockQuantity(product.getStockQuantity() + line.getQuantity());
                products.save(product);
            }
        }
        order.setOrderStatus(status);
        if ("DELIVERED".equals(status) && "CASH_ON_DELIVERY".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
            payments.findByOrder_Id(id).ifPresent(payment -> {
                payment.setPaymentStatus("PAID");
                payments.save(payment);
            });
        }
        deliveries.findByOrder_Id(id).ifPresent(delivery -> {
            if ("CANCELLED".equals(status)) delivery.setDeliveryStatus("CANCELLED");
            else if ("OUT_FOR_DELIVERY".equals(status)) delivery.setDeliveryStatus("OUT_FOR_DELIVERY");
            else if ("DELIVERED".equals(status)) {
                delivery.setDeliveryStatus("DELIVERED");
                delivery.setDeliveredAt(java.time.LocalDateTime.now());
            }
            deliveries.save(delivery);
        });
        CustomerOrder saved = orders.save(order);
        notify(order.getUser(), order, "Your order #" + order.getId() + " is now " + status.replace('_', ' ').toLowerCase() + ".");
        return view(saved);
    }

    private void notify(User user, CustomerOrder order, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setOrder(order);
        notification.setMessage(message);
        notifications.save(notification);
    }

    private Map<String, Object> view(CustomerOrder order) {
        List<Map<String, Object>> lines = orderItems.findByOrder_Id(order.getId()).stream().map(line -> {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", line.getProduct().getId());
            item.put("name", line.getProductName());
            item.put("capacity", line.getCapacity());
            item.put("quantity", line.getQuantity());
            item.put("unitPrice", line.getUnitPrice());
            item.put("subtotal", line.getSubtotal());
            return item;
        }).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("email", order.getUser().getEmail());
        result.put("status", order.getOrderStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("paymentMethod", order.getPaymentMethod());
        result.put("itemSubtotal", order.getItemSubtotal());
        result.put("deliveryFee", order.getDeliveryFee());
        result.put("totalAmount", order.getTotalAmount());
        result.put("orderDate", order.getOrderDate());
        result.put("deliveryAddress", order.getDeliveryAddress());
        result.put("items", lines);
        return result;
    }

    private String formatAddress(Address address) {
        return String.join(", ", List.of(address.getFullName(), address.getAddressLine(),
                address.getArea() == null ? "" : address.getArea(), address.getCity(),
                address.getState(), address.getPincode()).stream().filter(value -> !value.isBlank()).toList());
    }

    private double round(double amount) { return Math.round(amount * 100.0) / 100.0; }

    public record CheckoutRequest(Long addressId, String paymentMethod) {}
}