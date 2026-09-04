package com.wms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "wms_putaway_item")
public class PutawayItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "putaway_order_id", nullable = false)
    private PutawayOrder putawayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private int qty;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public PutawayItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PutawayOrder getPutawayOrder() { return putawayOrder; }
    public void setPutawayOrder(PutawayOrder putawayOrder) { this.putawayOrder = putawayOrder; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
