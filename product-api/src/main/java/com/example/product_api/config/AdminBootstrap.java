package com.example.product_api.config;

import com.example.product_api.model.User;
import com.example.product_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner provisionAdmin(UserRepository users) {
        return args -> {
            String email = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");
            if (email == null || email.isBlank() || password == null || password.length() < 12) return;
            users.findByEmailIgnoreCase(email.trim()).ifPresentOrElse(user -> {
                if (!"ADMIN".equals(user.getRole())) {
                    user.setRole("ADMIN");
                    users.save(user);
                }
            }, () -> {
                User admin = new User(email.trim().toLowerCase(), new BCryptPasswordEncoder().encode(password));
                admin.setRole("ADMIN");
                users.save(admin);
            });
        };
    }
}