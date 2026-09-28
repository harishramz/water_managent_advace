package com.example.product_api.controller;

import com.example.product_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orders;

    public AdminOrderController(OrderService orders) { this.orders = orders; }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        return isAdmin(session) ? ResponseEntity.ok(orders.listAll()) : forbidden();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            return ResponseEntity.ok(orders.updateStatus(id, request.status()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private boolean isAdmin(HttpSession session) { return "ADMIN".equals(session.getAttribute("role")); }
    private ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Admin access required")); }
    public record StatusRequest(String status) {}
}