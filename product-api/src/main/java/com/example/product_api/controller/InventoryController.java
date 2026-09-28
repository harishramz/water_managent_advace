package com.example.product_api.controller;

import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {
    private final ProductRepository products;
    public InventoryController(ProductRepository products) { this.products = products; }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean lowStock,
                                  @RequestParam(required = false) Boolean outOfStock,
                                  HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(products.findAll().stream().filter(product -> "ACTIVE".equals(product.getStatus()))
                .filter(product -> !Boolean.TRUE.equals(lowStock) || product.getStockQuantity() < 10)
                .filter(product -> !Boolean.TRUE.equals(outOfStock) || product.getStockQuantity() == 0)
                .toList());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> update(@PathVariable Long productId, @RequestBody StockRequest request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (request == null || request.stockQuantity() == null || request.stockQuantity() < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Stock quantity cannot be negative"));
        }
        return products.findById(productId).map(product -> {
            product.setStockQuantity(request.stockQuantity());
            return ResponseEntity.ok(products.save(product));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAdmin(HttpSession session) { return "ADMIN".equals(session.getAttribute("role")); }
    private ResponseEntity<?> forbidden() { return ResponseEntity.status(403).body(Map.of("message", "Admin access required")); }
    public record StockRequest(Integer stockQuantity) {}
}