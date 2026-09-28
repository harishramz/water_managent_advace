package com.example.product_api.controller;

import com.example.product_api.repository.PaymentRepository;
import com.example.product_api.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final UserRepository users;
    public PaymentController(PaymentRepository payments, UserRepository users) { this.payments = payments; this.users = users; }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> list(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long id)) return ResponseEntity.status(401).body(Map.of("message", "Please log in"));
        boolean admin = "ADMIN".equals(session.getAttribute("role"));
        if (!admin && !"CUSTOMER".equals(session.getAttribute("role"))) return ResponseEntity.status(403).body(Map.of("message", "Payment access denied"));
        return ResponseEntity.ok(payments.findAll().stream()
                .filter(payment -> admin || payment.getUser().getId().equals(id))
                .map(payment -> Map.of("id", payment.getId(), "orderId", payment.getOrder().getId(),
                        "amount", payment.getAmount(), "paymentMethod", payment.getPaymentMethod(),
                        "paymentStatus", payment.getPaymentStatus(), "paymentDate", payment.getPaymentDate()))
                .toList());
    }
}