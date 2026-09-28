package com.example.product_api.controller;

import com.example.product_api.repository.NotificationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications;
    public NotificationController(NotificationRepository notifications) { this.notifications = notifications; }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return ResponseEntity.ok(notifications.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(item ->
                Map.of("id", item.getId(), "orderId", item.getOrder() == null ? 0L : item.getOrder().getId(),
                        "message", item.getMessage(), "read", item.isRead(), "createdAt", item.getCreatedAt())).toList());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return notifications.findByIdAndUser_Id(id, userId).map(item -> {
            item.setRead(true);
            notifications.save(item);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Long customerId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "CUSTOMER".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Please log in")); }
}