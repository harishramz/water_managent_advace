package com.example.product_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

    private String description;

    private String waterType;
    private String capacity;
    private String brand;
    private String image;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer stockQuantity = 0;

    @Column(nullable = false, columnDefinition = "varchar(32) default 'ACTIVE'")
    private String status = "ACTIVE";

    public Product() {}

    public Product(String name, Double price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWaterType() { return waterType; }
    public void setWaterType(String waterType) { this.waterType = waterType; }
    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}