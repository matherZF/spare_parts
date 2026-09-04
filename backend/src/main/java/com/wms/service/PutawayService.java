package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.PutawayConfirmReq;
import com.wms.entity.*;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.PutawayOrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class PutawayService {
    private final PutawayOrderRepository orderRepo;
    private final LocationRepository locationRepo;
    private final InventoryRepository inventoryRepo;
    private final InventoryLogService inventoryLogService;

    public PutawayService(PutawayOrderRepository orderRepo,
                          LocationRepository locationRepo,
                          InventoryRepository inventoryRepo,
                          InventoryLogService inventoryLogService) {
        this.orderRepo = orderRepo;
        this.locationRepo = locationRepo;
        this.inventoryRepo = inventoryRepo;
        this.inventoryLogService = inventoryLogService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(PutawayConfirmReq req) {
        Integer qty = req.qty();
        if (qty == null || qty <= 0) throw new BizException("数量必须大于0");
        if (req.orderId() == null) throw new BizException("orderId不能为空");
        if (req.locationId() == null) throw new BizException("locationId不能为空");

        PutawayOrder order = orderRepo.findById(req.orderId())
                .orElseThrow(() -> new BizException("上架单不存在"));
        if (order.getStatus() == PutawayStatus.DONE) {
            throw new BizException("该上架单已完成，无需继续上架");
        }
        if (order.getPutQty() + qty > order.getPlanQty()) {
            int remain = order.getPlanQty() - order.getPutQty();
            throw new BizException(String.format("超过计划数量，剩余%d件", remain));
        }
        Location loc = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new BizException("库位不存在"));

        PutawayItem item = new PutawayItem();
        item.setPutawayOrder(order);
        item.setLocation(loc);
        item.setQty(qty);
        item.setCreatedAt(Instant.now());
        order.getItems().add(item);

        order.setPutQty(order.getPutQty() + qty);
        if (order.getPutQty() == order.getPlanQty()) {
            order.setStatus(PutawayStatus.DONE);
        }

        orderRepo.save(order);

        Product product = order.getProduct();
        Batch batch = order.getBatch();
        Long batchId = batch != null ? batch.getId() : null;
        // 按 商品+库位+批次 定位库存记录
        Inventory inv;
        if (batchId != null) {
            inv = inventoryRepo.findByProductIdAndLocationIdAndBatchId(product.getId(), loc.getId(), batchId).orElse(null);
        } else {
            inv = inventoryRepo.findByProductIdAndLocationId(product.getId(), loc.getId()).orElse(null);
        }
        int beforeQty = inv == null ? 0 : inv.getQty();
        int afterQty = beforeQty + qty;
        if (inv == null) {
            inv = new Inventory(product, loc, batch, qty, Instant.now());
        } else {
            inv.setQty(afterQty);
        }
        inventoryRepo.save(inv);

        // 记录入库日志（含批次信息）
        inventoryLogService.record(
                product.getId(), product.getSku(), product.getName(),
                loc.getId(), loc.getCode(),
                "INBOUND", qty, beforeQty, afterQty,
                "PUTAWAY", order.getOrderNo(),
                currentOperator(), null,
                batch != null ? batch.getItemKey() : null,
                batch != null ? batch.getProductionDate() : null,
                batch != null ? batch.getShelfLifeDays() : null,
                batch != null ? batch.getManufacturer() : null
        );

        boolean done = order.getStatus() == PutawayStatus.DONE;
        String msg = done ? "上架完成，订单已结束" : "上架成功，数量 " + qty;
        return Map.of(
                "orderId", order.getId(),
                "putQty", order.getPutQty(),
                "remaining", order.getPlanQty() - order.getPutQty(),
                "done", done,
                "msg", msg
        );
    }

    private String currentOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return String.valueOf(auth.getPrincipal());
    }
}
