package com.example.product_api.controller;

import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String waterType,
            @RequestParam(required = false) String capacity,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean available) {
        return productRepository.findAll().stream()
                .filter(product -> "ACTIVE".equals(product.getStatus()))
                .filter(product -> search == null || search.isBlank()
                        || product.getName().toLowerCase().contains(search.toLowerCase())
                        || (product.getDescription() != null && product.getDescription().toLowerCase().contains(search.toLowerCase()))
                        || (product.getBrand() != null && product.getBrand().toLowerCase().contains(search.toLowerCase())))
                .filter(product -> waterType == null || waterType.equalsIgnoreCase(product.getWaterType()))
                .filter(product -> capacity == null || capacity.equalsIgnoreCase(product.getCapacity()))
                .filter(product -> brand == null || product.getBrand() != null && product.getBrand().toLowerCase().contains(brand.toLowerCase()))
                .filter(product -> minPrice == null || product.getPrice() >= minPrice)
                .filter(product -> maxPrice == null || product.getPrice() <= maxPrice)
                .filter(product -> !Boolean.TRUE.equals(available) || product.getStockQuantity() > 0)
                .toList();
    }

    @GetMapping("/brands")
    public List<Map<String, Object>> getBrands() {
        return productRepository.findAll().stream()
                .filter(product -> "ACTIVE".equals(product.getStatus()))
                .filter(product -> product.getBrand() != null && !product.getBrand().isBlank())
                .collect(Collectors.groupingBy(Product::getBrand, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "productCount", entry.getValue()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id).filter(product -> "ACTIVE".equals(product.getStatus()))
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body(java.util.Map.of("message", "Admin access required"));
        if (product.getName() == null || product.getName().isBlank() || product.getPrice() == null || product.getPrice() < 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Enter a product name and valid price"));
        }
        product.setStatus("ACTIVE");
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) product.setStockQuantity(0);
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody java.util.Map<String, Object> changes, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body(java.util.Map.of("message", "Admin access required"));
        return productRepository.findById(id).map(product -> {
            if (changes.containsKey("name")) product.setName(String.valueOf(changes.get("name")));
            if (changes.containsKey("description")) product.setDescription((String) changes.get("description"));
            if (changes.containsKey("price")) product.setPrice(number(changes.get("price"), product.getPrice()));
            if (changes.containsKey("waterType")) product.setWaterType((String) changes.get("waterType"));
            if (changes.containsKey("capacity")) product.setCapacity((String) changes.get("capacity"));
            if (changes.containsKey("brand")) product.setBrand((String) changes.get("brand"));
            if (changes.containsKey("image")) product.setImage((String) changes.get("image"));
            if (changes.containsKey("stockQuantity")) product.setStockQuantity((int) number(changes.get("stockQuantity"), product.getStockQuantity()));
            return ResponseEntity.ok(productRepository.save(product));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body(java.util.Map.of("message", "Admin access required"));
        return productRepository.findById(id).map(product -> {
            product.setStatus("INACTIVE");
            productRepository.save(product);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    private double number(Object value, double fallback) {
        try {
            double parsed = Double.parseDouble(String.valueOf(value));
            return Double.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }
}