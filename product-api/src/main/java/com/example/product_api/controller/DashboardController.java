package com.example.product_api.controller;

import com.example.product_api.model.CustomerOrder;
import com.example.product_api.repository.CustomerOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final CustomerOrderRepository orders;
    public DashboardController(CustomerOrderRepository orders) { this.orders = orders; }

    @GetMapping("/customer")
    public ResponseEntity<?> customer(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId) || !"CUSTOMER".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(401).body(Map.of("message", "Please log in"));
        }
        List<CustomerOrder> customerOrders = orders.findByUser_IdOrderByOrderDateDesc(userId);
        long delivered = customerOrders.stream().filter(order -> "DELIVERED".equals(order.getOrderStatus())).count();
        long active = customerOrders.stream().filter(order -> !Set.of("DELIVERED", "CANCELLED").contains(order.getOrderStatus())).count();
        double spent = customerOrders.stream().filter(order -> "PAID".equals(order.getPaymentStatus()))
                .mapToDouble(CustomerOrder::getTotalAmount).sum();
        List<Long> recentOrderIds = customerOrders.stream().limit(5).map(CustomerOrder::getId).toList();
        return ResponseEntity.ok(Map.of("totalOrders", customerOrders.size(), "activeOrders", active,
                "deliveredOrders", delivered, "pendingOrders", customerOrders.stream()
                        .filter(order -> Set.of("PLACED", "CONFIRMED", "PROCESSING", "PACKED").contains(order.getOrderStatus())).count(),
                "totalSpent", spent, "recentOrderIds", recentOrderIds));
    }
}