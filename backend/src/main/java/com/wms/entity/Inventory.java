package com.wms.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "wms_inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "location_id"})
})
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private int qty;

    private Instant updatedAt;

    public Inventory() {}
    public Inventory(Product product, Location location, int qty, Instant updatedAt) {
        this.product = product;
        this.location = location;
        this.qty = qty;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    void preUpsert() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
