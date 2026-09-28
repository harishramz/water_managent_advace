package com.example.product_api.controller;

import com.example.product_api.model.User;
import com.example.product_api.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()
                || user.getPassword() == null || user.getPassword().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Enter a valid email and a password with at least 8 characters"));
        }
        String normalizedEmail = user.getEmail().trim().toLowerCase();
        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid email address"));
        }
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already registered"));
        }
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("CUSTOMER");
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body, HttpServletRequest request, HttpSession session) {
        String email    = body.get("email");
        String password = body.get("password");
        Optional<User> userOpt = email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email.trim());
        if (userOpt.isEmpty() || password == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid email or password"));
        }
        User user = userOpt.get();
        boolean hashedPassword = user.getPassword().startsWith("$2");
        if (user.getStatus().equals("DISABLED") || (hashedPassword
                ? !passwordEncoder.matches(password, user.getPassword())
                : !user.getPassword().equals(password))) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid email or password"));
        }
        if (!hashedPassword) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }
        request.changeSessionId();
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        return ResponseEntity.ok(Map.of("message", "Login successful",
                                        "email", user.getEmail(),
                                        "role", user.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            return ResponseEntity.status(401).body(Map.of("message", "Please log in"));
        }
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(), "name", user.getName() == null ? "" : user.getName(),
                        "email", user.getEmail(), "phone", user.getPhone() == null ? "" : user.getPhone(), "role", user.getRole())))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Please log in")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}