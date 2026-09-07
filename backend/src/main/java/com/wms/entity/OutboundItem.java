package com.wms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "wms_outbound_item")
public class OutboundItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_order_id", nullable = false)
    private OutboundOrder outboundOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    /** 拣货来源批次 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false)
    private int requestedQty;

    @Column(nullable = false)
    private int pickedQty;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public OutboundItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OutboundOrder getOutboundOrder() { return outboundOrder; }
    public void setOutboundOrder(OutboundOrder outboundOrder) { this.outboundOrder = outboundOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }
    public int getRequestedQty() { return requestedQty; }
    public void setRequestedQty(int requestedQty) { this.requestedQty = requestedQty; }
    public int getPickedQty() { return pickedQty; }
    public void setPickedQty(int pickedQty) { this.pickedQty = pickedQty; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
