package com.wms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="wms_equipment_part", uniqueConstraints=@UniqueConstraint(columnNames={"equipment_id","product_id"}))
public class EquipmentPart {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="equipment_id", nullable=false) private Equipment equipment;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id", nullable=false) private Product product;
    @Column(nullable=false) private int installedQty = 1;
    private Integer lifeCycleDays;
    private Integer consumptionCycleDays;
    private Integer procurementLeadDays;
    @Column(nullable=false) private int safetyStock = 0;
    private LocalDate lastUsedDate;
    private LocalDate nextExpectedUseDate;
    public Long getId(){return id;} public Equipment getEquipment(){return equipment;} public void setEquipment(Equipment v){equipment=v;} public Product getProduct(){return product;} public void setProduct(Product v){product=v;}
    public int getInstalledQty(){return installedQty;} public void setInstalledQty(int v){installedQty=v;} public Integer getLifeCycleDays(){return lifeCycleDays;} public void setLifeCycleDays(Integer v){lifeCycleDays=v;}
    public Integer getConsumptionCycleDays(){return consumptionCycleDays;} public void setConsumptionCycleDays(Integer v){consumptionCycleDays=v;} public Integer getProcurementLeadDays(){return procurementLeadDays;} public void setProcurementLeadDays(Integer v){procurementLeadDays=v;}
    public int getSafetyStock(){return safetyStock;} public void setSafetyStock(int v){safetyStock=v;} public LocalDate getLastUsedDate(){return lastUsedDate;} public void setLastUsedDate(LocalDate v){lastUsedDate=v;} public LocalDate getNextExpectedUseDate(){return nextExpectedUseDate;} public void setNextExpectedUseDate(LocalDate v){nextExpectedUseDate=v;}
}
