package com.wms.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "wms_product", uniqueConstraints = {
        @UniqueConstraint(columnNames = "sku")
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 255)
    private String spec;

    @Column(length = 32)
    private String unit;

    @Column(updatable = false)
    private Instant createdAt;

    public Product() {}
    public Product(String sku, String name, String spec, String unit) {
        this.sku = sku;
        this.name = name;
        this.spec = spec;
        this.unit = unit;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
