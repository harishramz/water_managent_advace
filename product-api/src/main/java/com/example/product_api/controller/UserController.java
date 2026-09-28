package com.example.product_api.controller;

import com.example.product_api.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileRequest request, HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId) || !"CUSTOMER".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Please log in"));
        }
        if (request == null || request.name() == null || request.name().isBlank()
                || request.phone() == null || !request.phone().matches("[0-9+() -]{8,18}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter your name and a valid phone number"));
        }
        return users.findById(userId).map(user -> {
            user.setName(request.name().trim());
            user.setPhone(request.phone().trim());
            users.save(user);
            return ResponseEntity.ok(Map.of("id", user.getId(), "name", user.getName(),
                    "email", user.getEmail(), "phone", user.getPhone(), "role", user.getRole()));
        }).orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Please log in")));
    }

    public record ProfileRequest(String name, String phone) {}
}