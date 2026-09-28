package com.example.product_api.controller;

import com.example.product_api.model.CartItem;
import com.example.product_api.model.Product;
import com.example.product_api.model.User;
import com.example.product_api.repository.CartItemRepository;
import com.example.product_api.repository.ProductRepository;
import com.example.product_api.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@Transactional
public class CartController {
    private final CartItemRepository items;
    private final ProductRepository products;
    private final UserRepository users;

    public CartController(CartItemRepository items, ProductRepository products, UserRepository users) {
        this.items = items;
        this.products = products;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<?> getCart(HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return ResponseEntity.ok(view(items.findByUser_Id(userId)));
    }

    @PostMapping("/items")
    @Transactional
    public ResponseEntity<?> add(@RequestBody CartRequest request, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        if (request == null || request.productId() == null || request.quantity() == null || request.quantity() < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Quantity must be at least 1"));
        }
        Product product = products.findById(request.productId()).filter(p -> "ACTIVE".equals(p.getStatus())).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();
        CartItem item = items.findByUser_IdAndProduct_Id(userId, product.getId()).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUser(users.findById(userId).orElseThrow());
            created.setProduct(product);
            created.setQuantity(0);
            return created;
        });
        if (item.getQuantity() + request.quantity() > product.getStockQuantity()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only " + product.getStockQuantity() + " units are currently available."));
        }
        item.setQuantity(item.getQuantity() + request.quantity());
        items.save(item);
        return ResponseEntity.ok(view(items.findByUser_Id(userId)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CartRequest request, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        if (request == null || request.quantity() == null || request.quantity() < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Quantity must be at least 1"));
        }
        return items.findByIdAndUser_Id(id, userId).map(item -> {
            if (request.quantity() > item.getProduct().getStockQuantity()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only " + item.getProduct().getStockQuantity() + " units are currently available."));
            }
            item.setQuantity(request.quantity());
            items.save(item);
            return ResponseEntity.ok(view(items.findByUser_Id(userId)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return items.findByIdAndUser_Id(id, userId).map(item -> {
            items.delete(item);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<?> clear(HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        items.deleteByUser_Id(userId);
        return ResponseEntity.noContent().build();
    }

    private Long customerId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "CUSTOMER".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }

    private Map<String, Object> view(List<CartItem> cartItems) {
        List<Map<String, Object>> lines = cartItems.stream().map(item -> {
            Product product = item.getProduct();
            Map<String, Object> line = new HashMap<>();
            line.put("id", item.getId());
            line.put("productId", product.getId());
            line.put("name", product.getName());
            line.put("brand", product.getBrand() == null ? "" : product.getBrand());
            line.put("capacity", product.getCapacity());
            line.put("quantity", item.getQuantity());
            line.put("unitPrice", product.getPrice());
            line.put("subtotal", product.getPrice() * item.getQuantity());
            line.put("availableStock", product.getStockQuantity());
            return line;
        }).toList();
        double subtotal = lines.stream().mapToDouble(line -> (double) line.get("subtotal")).sum();
        int itemCount = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        return Map.of("items", lines, "itemCount", itemCount, "subtotal", subtotal, "deliveryCharge", subtotal == 0 || subtotal >= 500 ? 0 : 30,
                "total", subtotal + (subtotal == 0 || subtotal >= 500 ? 0 : 30));
    }

    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Please log in")); }

    public record CartRequest(Long productId, Integer quantity) {}
}