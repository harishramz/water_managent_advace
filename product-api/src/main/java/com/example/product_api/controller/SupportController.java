package com.example.product_api.controller;

import com.example.product_api.model.CustomerOrder;
import com.example.product_api.model.SupportRequest;
import com.example.product_api.repository.CustomerOrderRepository;
import com.example.product_api.repository.SupportRequestRepository;
import com.example.product_api.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportRequestRepository requests;
    private final CustomerOrderRepository orders;
    private final UserRepository users;
    public SupportController(SupportRequestRepository requests, CustomerOrderRepository orders, UserRepository users) {
        this.requests = requests;
        this.orders = orders;
        this.users = users;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody SupportForm form, HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        if (form == null || form.subject() == null || form.subject().isBlank()
                || form.description() == null || form.description().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Add a subject and describe the issue"));
        }
        SupportRequest request = new SupportRequest();
        request.setUser(users.findById(userId).orElseThrow());
        if (form.orderId() != null) {
            CustomerOrder order = orders.findByIdAndUser_Id(form.orderId(), userId).orElse(null);
            if (order == null) return ResponseEntity.badRequest().body(Map.of("message", "That order could not be found"));
            request.setOrder(order);
        }
        request.setSubject(form.subject().trim());
        request.setDescription(form.description().trim());
        return ResponseEntity.ok(view(requests.save(request)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listCustomer(HttpSession session) {
        Long userId = customerId(session);
        if (userId == null) return unauthorized();
        return ResponseEntity.ok(requests.findByUser_IdOrderByCreatedAtDesc(userId).stream().map(this::view).toList());
    }

    @GetMapping("/admin")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listAdmin(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(requests.findAll().stream().map(this::view).toList());
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusForm form, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (form == null || !Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(form.status())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid support status"));
        }
        return requests.findById(id).map(request -> {
            request.setStatus(form.status());
            request.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(view(requests.save(request)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> view(SupportRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", request.getId());
        result.put("email", request.getUser().getEmail());
        result.put("orderId", request.getOrder() == null ? null : request.getOrder().getId());
        result.put("subject", request.getSubject());
        result.put("description", request.getDescription());
        result.put("status", request.getStatus());
        result.put("createdAt", request.getCreatedAt());
        result.put("updatedAt", request.getUpdatedAt());
        return result;
    }
    private Long customerId(HttpSession session) {
        Object id = session.getAttribute("userId");
        return "CUSTOMER".equals(session.getAttribute("role")) && id instanceof Long userId ? userId : null;
    }
    private boolean isAdmin(HttpSession session) { return "ADMIN".equals(session.getAttribute("role")); }
    private ResponseEntity<?> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Please log in")); }
    private ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Admin access required")); }
    public record SupportForm(Long orderId, String subject, String description) {}
    public record StatusForm(String status) {}
}