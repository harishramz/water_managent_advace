package com.example.product_api.controller;

import com.example.product_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody OrderService.CheckoutRequest request, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(orders.place(userId, request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Long userId = customerId(session);
        return userId == null ? unauthorized() : ResponseEntity.ok(orders.listForCustomer(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(orders.getForCustomer(id, userId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(404).body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(orders.cancel(id, userId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{id}/reorder")
    public ResponseEntity<?> reorder(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(orders.reorder(id, userId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private Long customerId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "CUSTOMER".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }

    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Please log in")); }
}