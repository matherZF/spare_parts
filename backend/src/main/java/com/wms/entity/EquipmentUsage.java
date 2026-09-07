package com.wms.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
@Entity @Table(name="wms_equipment_usage")
public class EquipmentUsage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="equipment_id",nullable=false) private Equipment equipment;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private Product product;
 @Column(nullable=false) private int qty; @Column(length=64) private String outboundOrderNo;
 private Integer lifeCycleDays; private LocalDate nextExpectedUseDate; @CreationTimestamp private Instant usedAt;
 public void setEquipment(Equipment v){equipment=v;} public void setProduct(Product v){product=v;} public void setQty(int v){qty=v;} public void setOutboundOrderNo(String v){outboundOrderNo=v;} public void setLifeCycleDays(Integer v){lifeCycleDays=v;} public void setNextExpectedUseDate(LocalDate v){nextExpectedUseDate=v;}
}
