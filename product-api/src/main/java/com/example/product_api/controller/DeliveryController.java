package com.example.product_api.controller;

import com.example.product_api.model.Delivery;
import com.example.product_api.model.User;
import com.example.product_api.repository.DeliveryRepository;
import com.example.product_api.repository.UserRepository;
import com.example.product_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class DeliveryController {
    private final DeliveryRepository deliveries;
    private final UserRepository users;
    private final OrderService orders;

    public DeliveryController(DeliveryRepository deliveries, UserRepository users, OrderService orders) {
        this.deliveries = deliveries;
        this.users = users;
        this.orders = orders;
    }

    @GetMapping("/api/admin/deliveries")
    @Transactional(readOnly = true)
    public ResponseEntity<?> all(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(deliveries.findAll().stream().map(this::view).toList());
    }

    @GetMapping("/api/admin/delivery-staff")
    public ResponseEntity<?> staff(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(users.findAll().stream()
                .filter(user -> "DELIVERY".equals(user.getRole()) && "ACTIVE".equals(user.getStatus()))
                .map(user -> Map.of("id", user.getId(), "name", user.getName() == null ? "" : user.getName(), "email", user.getEmail()))
                .toList());
    }

    @PostMapping("/api/admin/delivery-staff")
    public ResponseEntity<?> createStaff(@RequestBody StaffRequest request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (request == null || request.email() == null || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || request.name() == null || request.name().isBlank()
                || request.password() == null || request.password().length() < 12) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a name, valid email, and password with at least 12 characters"));
        }
        String email = request.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        User user = new User(email, new BCryptPasswordEncoder().encode(request.password()));
        user.setName(request.name().trim());
        user.setRole("DELIVERY");
        user.setStatus("ACTIVE");
        return ResponseEntity.ok(Map.of("id", users.save(user).getId(), "name", user.getName(), "email", email));
    }

    @PutMapping("/api/admin/deliveries/{id}/assign")
    @Transactional
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody AssignRequest request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (request == null || request.deliveryPersonId() == null) return ResponseEntity.badRequest().body(Map.of("message", "Choose a delivery person"));
        User staff = users.findById(request.deliveryPersonId()).filter(user -> "DELIVERY".equals(user.getRole()) && "ACTIVE".equals(user.getStatus())).orElse(null);
        if (staff == null) return ResponseEntity.badRequest().body(Map.of("message", "Active delivery staff account not found"));
        return deliveries.findById(id).map(delivery -> {
            delivery.setDeliveryPerson(staff);
            delivery.setDeliveryStatus("ASSIGNED");
            return ResponseEntity.ok(view(deliveries.save(delivery)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/deliveries/assigned")
    @Transactional(readOnly = true)
    public ResponseEntity<?> assigned(HttpSession session) {
        Long userId = deliveryId(session);
        if (userId == null) return unauthorized();
        return ResponseEntity.ok(deliveries.findByDeliveryPerson_IdOrderByAssignedAtAsc(userId).stream().map(this::view).toList());
    }

    @PutMapping("/api/deliveries/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody DeliveryStatusRequest request, HttpSession session) {
        Long userId = deliveryId(session);
        if (userId == null) return unauthorized();
        Set<String> allowed = Set.of("PICKED_UP", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED");
        if (request == null || !allowed.contains(request.status())) return ResponseEntity.badRequest().body(Map.of("message", "Invalid delivery status"));
        return deliveries.findByIdAndDeliveryPerson_Id(id, userId).map(delivery -> {
            delivery.setDeliveryStatus(request.status());
            if ("DELIVERED".equals(request.status())) {
                delivery.setDeliveredAt(LocalDateTime.now());
                orders.updateStatus(delivery.getOrder().getId(), "DELIVERED");
            } else if ("OUT_FOR_DELIVERY".equals(request.status()) && "PACKED".equals(delivery.getOrder().getOrderStatus())) {
                orders.updateStatus(delivery.getOrder().getId(), "OUT_FOR_DELIVERY");
            }
            return ResponseEntity.ok(view(deliveries.save(delivery)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> view(Delivery delivery) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", delivery.getId());
        result.put("orderId", delivery.getOrder().getId());
        result.put("orderStatus", delivery.getOrder().getOrderStatus());
        result.put("deliveryStatus", delivery.getDeliveryStatus());
        result.put("deliveryPersonId", delivery.getDeliveryPerson() == null ? null : delivery.getDeliveryPerson().getId());
        result.put("deliveryPerson", delivery.getDeliveryPerson() == null ? "" : delivery.getDeliveryPerson().getName());
        result.put("customerEmail", delivery.getOrder().getUser().getEmail());
        result.put("customerPhone", delivery.getOrder().getAddress().getPhone());
        result.put("address", delivery.getOrder().getDeliveryAddress());
        result.put("totalAmount", delivery.getOrder().getTotalAmount());
        result.put("assignedAt", delivery.getAssignedAt());
        result.put("deliveredAt", delivery.getDeliveredAt());
        return result;
    }

    private boolean isAdmin(HttpSession session) { return "ADMIN".equals(session.getAttribute("role")); }
    private Long deliveryId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "DELIVERY".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }
    private ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Admin access required")); }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Delivery staff sign-in required")); }
    public record StaffRequest(String name, String email, String password) {}
    public record AssignRequest(Long deliveryPersonId) {}
    public record DeliveryStatusRequest(String status) {}
}