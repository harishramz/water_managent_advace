package com.example.product_api.controller;

import com.example.product_api.model.CustomerOrder;
import com.example.product_api.model.Product;
import com.example.product_api.model.User;
import com.example.product_api.repository.CustomerOrderRepository;
import com.example.product_api.repository.ProductRepository;
import com.example.product_api.repository.UserRepository;
import com.example.product_api.repository.OrderItemRepository;
import com.example.product_api.repository.DeliveryRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final CustomerOrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderItemRepository orderItems;
    private final DeliveryRepository deliveries;
    public AdminDashboardController(CustomerOrderRepository orders, ProductRepository products, UserRepository users,
                                    OrderItemRepository orderItems, DeliveryRepository deliveries) {
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.orderItems = orderItems;
        this.deliveries = deliveries;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<?> dashboard(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        List<CustomerOrder> allOrders = orders.findAll();
        List<Product> allProducts = products.findAll();
        LocalDate today = LocalDate.now();
        double revenue = allOrders.stream().filter(order -> "PAID".equals(order.getPaymentStatus()))
                .mapToDouble(CustomerOrder::getTotalAmount).sum();
        double todayRevenue = allOrders.stream().filter(order -> order.getOrderDate().toLocalDate().equals(today))
                .filter(order -> "PAID".equals(order.getPaymentStatus())).mapToDouble(CustomerOrder::getTotalAmount).sum();
        List<Map<String, Object>> recent = allOrders.stream().sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .limit(8).map(order -> Map.<String, Object>of("id", order.getId(), "email", order.getUser().getEmail(),
                        "status", order.getOrderStatus(), "paymentStatus", order.getPaymentStatus(),
                        "totalAmount", order.getTotalAmount(), "orderDate", order.getOrderDate())).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("totalCustomers", users.findAll().stream().filter(user -> "CUSTOMER".equals(user.getRole())).count());
        result.put("totalOrders", allOrders.size());
        result.put("pendingOrders", allOrders.stream().filter(order -> Set.of("PLACED", "CONFIRMED", "PROCESSING", "PACKED").contains(order.getOrderStatus())).count());
        result.put("deliveredOrders", allOrders.stream().filter(order -> "DELIVERED".equals(order.getOrderStatus())).count());
        result.put("cancelledOrders", allOrders.stream().filter(order -> "CANCELLED".equals(order.getOrderStatus())).count());
        result.put("activeDeliveries", allOrders.stream().filter(order -> "OUT_FOR_DELIVERY".equals(order.getOrderStatus())).count());
        result.put("totalRevenue", revenue);
        result.put("todayOrders", allOrders.stream().filter(order -> order.getOrderDate().toLocalDate().equals(today)).count());
        result.put("todayRevenue", todayRevenue);
        result.put("waterProducts", allProducts.stream().filter(product -> "ACTIVE".equals(product.getStatus())).count());
        result.put("lowStockProducts", allProducts.stream().filter(product -> "ACTIVE".equals(product.getStatus()) && product.getStockQuantity() < 10).count());
        result.put("outOfStockProducts", allProducts.stream().filter(product -> "ACTIVE".equals(product.getStatus()) && product.getStockQuantity() == 0).count());
        result.put("recentOrders", recent);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers")
    @Transactional(readOnly = true)
    public ResponseEntity<?> customers(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(users.findAll().stream().filter(user -> "CUSTOMER".equals(user.getRole()))
                .map(this::customerView).toList());
    }

        @GetMapping("/reports")
        @Transactional(readOnly = true)
        public ResponseEntity<?> reports(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        List<CustomerOrder> allOrders = orders.findAll();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> dailyOrders = java.util.stream.IntStream.rangeClosed(0, 6).mapToObj(offset -> {
            LocalDate date = today.minusDays(6L - offset);
            return Map.<String, Object>of("date", date.toString(), "orders", allOrders.stream()
                .filter(order -> order.getOrderDate().toLocalDate().equals(date)).count());
        }).toList();
        List<Map<String, Object>> monthlyRevenue = java.util.stream.IntStream.rangeClosed(0, 5).mapToObj(offset -> {
            YearMonth month = YearMonth.now().minusMonths(5L - offset);
            double amount = allOrders.stream().filter(order -> YearMonth.from(order.getOrderDate()).equals(month))
                .filter(order -> "PAID".equals(order.getPaymentStatus()))
                .mapToDouble(CustomerOrder::getTotalAmount).sum();
            return Map.<String, Object>of("month", month.format(DateTimeFormatter.ofPattern("MMM yyyy")), "revenue", amount);
        }).toList();
        Map<String, Long> demand = orderItems.findAll().stream()
            .filter(item -> !"CANCELLED".equals(item.getOrder().getOrderStatus()))
            .collect(java.util.stream.Collectors.groupingBy(item -> item.getProductName(),
                java.util.stream.Collectors.summingLong(item -> item.getQuantity())));
        Map<String, Long> deliveryStats = deliveries.findAll().stream().collect(java.util.stream.Collectors.groupingBy(
            delivery -> delivery.getDeliveryStatus(), java.util.stream.Collectors.counting()));
        return ResponseEntity.ok(Map.of("dailyOrders", dailyOrders, "monthlyRevenue", monthlyRevenue,
            "productDemand", demand, "deliveryStats", deliveryStats));
        }

    @PutMapping("/customers/{id}/status")
    public ResponseEntity<?> setCustomerStatus(@PathVariable Long id, @RequestBody StatusRequest request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (request == null || !Set.of("ACTIVE", "DISABLED").contains(request.status())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid customer status"));
        }
        return users.findById(id).filter(user -> "CUSTOMER".equals(user.getRole())).map(user -> {
            user.setStatus(request.status());
            users.save(user);
            return ResponseEntity.ok(customerView(user));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> customerView(User user) {
        return Map.of("id", user.getId(), "name", user.getName() == null ? "" : user.getName(),
                "email", user.getEmail(), "phone", user.getPhone() == null ? "" : user.getPhone(),
                "status", user.getStatus());
    }
    private boolean isAdmin(HttpSession session) { return "ADMIN".equals(session.getAttribute("role")); }
    private ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Admin access required")); }
    public record StatusRequest(String status) {}
}