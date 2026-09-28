package com.example.product_api.config;

import com.example.product_api.model.Product;
import com.example.product_api.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

@Configuration
public class CatalogBootstrap {
    private static final String BOTTLE_IMAGE = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=85";
    private static final String LARGE_CAN_IMAGE = "https://images.unsplash.com/photo-1564419320461-6870880221ad?auto=format&fit=crop&w=900&q=85";

    @Bean
    @ConditionalOnProperty(name = "catalog.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedWaterCatalog(ProductRepository products) {
        return args -> {
            if (products.count() > 0) return;
            List<Product> catalog = List.of(
                    product("Bisleri Mineral Water 1L", "Packaged drinking water bottle", "Packaged Drinking Water", "1L", "Bisleri", 20),
                    product("Bisleri Mineral Water 500ml", "Convenient single-serve drinking water", "Packaged Drinking Water", "500ml", "Bisleri", 10),
                    product("Kinley Packaged Water 1L", "Packaged drinking water bottle", "Packaged Drinking Water", "1L", "Kinley", 20),
                    product("Aquafina Packaged Water 1L", "Packaged drinking water bottle", "Packaged Drinking Water", "1L", "Aquafina", 20),
                    product("Bailley Packaged Water 1L", "Packaged drinking water bottle", "Packaged Drinking Water", "1L", "Bailley", 20),
                    product("Himalayan Natural Mineral Water 1L", "Natural mineral water bottle", "Natural Mineral Water", "1L", "Himalayan", 75),
                    product("Tata Copper+ Water 1L", "Packaged drinking water in a copper-charged bottle", "Packaged Drinking Water", "1L", "Tata Copper+", 30),
                    product("Rail Neer Packaged Water 1L", "Packaged drinking water bottle", "Packaged Drinking Water", "1L", "Rail Neer", 15),
                    product("Vedica Himalayan Spring Water 750ml", "Natural spring water bottle", "Spring Water", "750ml", "Vedica", 60),
                    product("Bisleri 20L Water Can", "Reusable large-format packaged drinking water can", "Packaged Drinking Water", "20L", "Bisleri", 90)
            );
            products.saveAll(catalog);
        };
    }

    private Product product(String name, String description, String type, String capacity, String brand, double suggestedPrice) {
        Product product = new Product(name, suggestedPrice, description);
        product.setWaterType(type);
        product.setCapacity(capacity);
        product.setBrand(brand);
        product.setImage("20L".equals(capacity) ? LARGE_CAN_IMAGE : BOTTLE_IMAGE);
        product.setStockQuantity(24);
        product.setStatus("ACTIVE");
        return product;
    }
}