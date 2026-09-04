package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.*;
import com.wms.entity.*;
import com.wms.repository.InventoryRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutboundService {
    private final OutboundOrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final InventoryRepository inventoryRepo;
    private final DeviceService deviceService;
    private final InventoryLogService inventoryLogService;

    public OutboundService(OutboundOrderRepository orderRepo,
                           ProductRepository productRepo,
                           InventoryRepository inventoryRepo,
                           DeviceService deviceService,
                           InventoryLogService inventoryLogService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.inventoryRepo = inventoryRepo;
        this.deviceService = deviceService;
        this.inventoryLogService = inventoryLogService;
    }

    public synchronized String generateOrderNo() {
        String prefix = "OUT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int nextSeq = 1;
        var opt = orderRepo.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix);
        if (opt.isPresent()) {
            String last = opt.get().getOrderNo();
            try {
                String seq = last.substring(prefix.length());
                nextSeq = Integer.parseInt(seq) + 1;
            } catch (Exception ignore) {}
        }
        return prefix + String.format("%04d", nextSeq);
    }

    @Transactional
    public OutboundOrderDetailVO create(OutboundCreateReq req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new BizException("请至少选择一个货品");
        }
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setStatus(OutboundStatus.PENDING);
        for (OutboundItemCreateReq ir : req.items()) {
            if (ir.requestedQty() == null || ir.requestedQty() <= 0) {
                throw new BizException("数量必须大于0");
            }
            Product product = productRepo.findById(ir.productId())
                    .orElseThrow(() -> new BizException("商品不存在: id=" + ir.productId()));
            OutboundItem item = new OutboundItem();
            item.setOutboundOrder(order);
            item.setProduct(product);
            item.setRequestedQty(ir.requestedQty());
            item.setPickedQty(0);
            order.getItems().add(item);
        }
        orderRepo.save(order);
        return toDetail(order);
    }

    public Page<OutboundOrderListItemVO> page(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        OutboundStatus s = null;
        if (StringUtils.hasText(status)) {
            try { s = OutboundStatus.valueOf(status.trim().toUpperCase()); }
            catch (Exception e) { throw new BizException("状态参数无效"); }
        }
        return orderRepo.findByStatus(s, pageable).map(this::toListItem);
    }

    public List<OutboundOrderListItemVO> pendingList() {
        return orderRepo.findPendingList(OutboundStatus.PENDING).stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OutboundOrderDetailVO detail(Long id) {
        return orderRepo.findDetailById(id).map(this::toDetail)
                .orElseThrow(() -> new BizException("领用单不存在"));
    }

    /**
     * 查询某商品的可用库存列表（含批次详情），按 FIFO（到期日/生产日期升序）排序。
     * 用于出库时人工选择库位/批次。
     */
    @Transactional(readOnly = true)
    public List<AvailableStockVO> listAvailableInventory(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new BizException("商品不存在"));
        List<Inventory> stocks = inventoryRepo.findAllByProductId(productId).stream()
                .filter(s -> s.getQty() > 0)
                .sorted(Comparator
                        .comparing((Inventory s) -> s.getBatch() != null ? s.getBatch().getExpiryDate() : null,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(s -> s.getBatch() != null ? s.getBatch().getProductionDate() : null,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(Inventory::getQty).reversed())
                .collect(Collectors.toList());
        return stocks.stream().map(s -> {
            Batch b = s.getBatch();
            return new AvailableStockVO(
                    s.getId(),
                    product.getId(), product.getSku(), product.getName(),
                    s.getLocation().getId(), s.getLocation().getCode(),
                    s.getLocation().getArea(), s.getLocation().getDeviceNo(),
                    s.getQty(),
                    b != null ? b.getId() : null,
                    b != null ? b.getItemKey() : null,
                    b != null ? b.getProductionDate() : null,
                    b != null ? b.getShelfLifeDays() : null,
                    b != null ? b.getManufacturer() : null,
                    b != null ? b.getExpiryDate() : null
            );
        }).collect(Collectors.toList());
    }

    /**
     * 人工为出库项分配库位+批次。
     */
    @Transactional
    public OutboundItemVO assignLocation(Long orderId, Long itemId, Long locationId, Long batchId) {
        OutboundOrder order = orderRepo.findDetailById(orderId)
                .orElseThrow(() -> new BizException("领用单不存在"));
        if (order.getStatus() != OutboundStatus.PENDING && order.getStatus() != OutboundStatus.PICKING) {
            throw new BizException("当前状态不可分配库位");
        }
        OutboundItem item = order.getItems().stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BizException("拣货项不存在"));
        if (item.getProduct() == null) {
            throw new BizException("货品信息缺失");
        }
        // 校验该库存记录存在且数量充足
        Inventory stock;
        if (batchId != null) {
            stock = inventoryRepo.findByProductIdAndLocationIdAndBatchId(
                    item.getProduct().getId(), locationId, batchId)
                    .orElseThrow(() -> new BizException("指定库位/批次的库存不存在"));
        } else {
            stock = inventoryRepo.findByProductIdAndLocationId(item.getProduct().getId(), locationId)
                    .orElseThrow(() -> new BizException("指定库位的库存不存在"));
        }
        if (stock.getQty() < item.getRequestedQty()) {
            throw new BizException(String.format("所选库存不足，仅剩 %d 件", stock.getQty()));
        }
        item.setLocation(stock.getLocation());
        item.setBatch(stock.getBatch());
        orderRepo.save(order);

        // 点亮对应库位灯光
        String deviceNo = deviceService.lightUp(stock.getLocation().getId());

        int avail = stock.getQty();
        Batch b = stock.getBatch();
        return new OutboundItemVO(
                item.getId(), item.getProduct().getId(), item.getProduct().getSku(),
                item.getProduct().getName(), item.getRequestedQty(), item.getPickedQty(),
                stock.getLocation().getId(), stock.getLocation().getCode(),
                stock.getLocation().getArea(), deviceNo, avail,
                b != null ? b.getId() : null,
                b != null ? b.getItemKey() : null,
                b != null ? b.getProductionDate() : null,
                b != null ? b.getShelfLifeDays() : null,
                b != null ? b.getManufacturer() : null,
                b != null ? b.getExpiryDate() : null
        );
    }

    /**
     * 开始拣货：为每个商品项按 FIFO 自动分配库位+批次，并点亮对应库位灯光设备。
     */
    @Transactional
    public Map<String, Object> startPicking(Long orderId) {
        OutboundOrder order = orderRepo.findDetailById(orderId)
                .orElseThrow(() -> new BizException("领用单不存在"));
        if (order.getStatus() != OutboundStatus.PENDING) {
            throw new BizException("仅待拣货状态可开始拣货");
        }
        List<Map<String, Object>> pickingItems = new ArrayList<>();
        for (OutboundItem item : order.getItems()) {
            // 若已人工分配库位+批次，则保留，仅点亮灯光
            if (item.getLocation() != null) {
                String deviceNo = deviceService.lightUp(item.getLocation().getId());
                Batch b = item.getBatch();
                Map<String, Object> mi = new LinkedHashMap<>();
                mi.put("itemId", item.getId());
                mi.put("productId", item.getProduct().getId());
                mi.put("sku", item.getProduct().getSku());
                mi.put("name", item.getProduct().getName());
                mi.put("requestedQty", item.getRequestedQty());
                mi.put("locationId", item.getLocation().getId());
                mi.put("locationCode", item.getLocation().getCode());
                mi.put("locationArea", item.getLocation().getArea());
                mi.put("deviceNo", deviceNo);
                int avail = 0;
                if (b != null) {
                    avail = inventoryRepo.findByProductIdAndLocationIdAndBatchId(
                            item.getProduct().getId(), item.getLocation().getId(), b.getId())
                            .map(Inventory::getQty).orElse(0);
                }
                mi.put("availableQty", avail);
                mi.put("batchId", b != null ? b.getId() : null);
                mi.put("itemKey", b != null ? b.getItemKey() : null);
                mi.put("productionDate", b != null ? b.getProductionDate() : null);
                mi.put("shelfLifeDays", b != null ? b.getShelfLifeDays() : null);
                mi.put("manufacturer", b != null ? b.getManufacturer() : null);
                mi.put("expiryDate", b != null ? b.getExpiryDate() : null);
                pickingItems.add(mi);
                continue;
            }
            // 找到该商品有库存的库位，按 FIFO 排序（到期日/生产日期升序），取第一个满足数量的
            List<Inventory> stocks = inventoryRepo.findAllByProductId(item.getProduct().getId()).stream()
                    .filter(s -> s.getQty() > 0)
                    .sorted(Comparator
                            .comparing((Inventory s) -> s.getBatch() != null ? s.getBatch().getExpiryDate() : null,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(s -> s.getBatch() != null ? s.getBatch().getProductionDate() : null,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparingInt(Inventory::getQty).reversed())
                    .collect(Collectors.toList());
            Inventory stock = stocks.stream()
                    .filter(s -> s.getQty() >= item.getRequestedQty())
                    .findFirst()
                    .orElse(null);
            if (stock == null) {
                // 没有单个批次满足，取库存最大的一个提示不足
                Inventory maxStock = stocks.stream().max(Comparator.comparingInt(Inventory::getQty)).orElse(null);
                throw new BizException(String.format("商品 %s 无可用库存（最大单批次 %d 件）",
                        item.getProduct().getSku(),
                        maxStock != null ? maxStock.getQty() : 0));
            }
            item.setLocation(stock.getLocation());
            item.setBatch(stock.getBatch());
            // 预留接口：点亮库位灯光设备
            String deviceNo = deviceService.lightUp(stock.getLocation().getId());
            Batch b = stock.getBatch();
            Map<String, Object> mi = new LinkedHashMap<>();
            mi.put("itemId", item.getId());
            mi.put("productId", item.getProduct().getId());
            mi.put("sku", item.getProduct().getSku());
            mi.put("name", item.getProduct().getName());
            mi.put("requestedQty", item.getRequestedQty());
            mi.put("locationId", stock.getLocation().getId());
            mi.put("locationCode", stock.getLocation().getCode());
            mi.put("locationArea", stock.getLocation().getArea());
            mi.put("deviceNo", deviceNo);
            mi.put("availableQty", stock.getQty());
            // 批次信息
            mi.put("batchId", b != null ? b.getId() : null);
            mi.put("itemKey", b != null ? b.getItemKey() : null);
            mi.put("productionDate", b != null ? b.getProductionDate() : null);
            mi.put("shelfLifeDays", b != null ? b.getShelfLifeDays() : null);
            mi.put("manufacturer", b != null ? b.getManufacturer() : null);
            mi.put("expiryDate", b != null ? b.getExpiryDate() : null);
            pickingItems.add(mi);
        }
        order.setStatus(OutboundStatus.PICKING);
        orderRepo.save(order);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("items", pickingItems);
        return result;
    }

    /**
     * 拣货完成：扣减库存，更新已拣数量，若全部完成则单据置为 DONE。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completePicking(Long orderId) {
        OutboundOrder order = orderRepo.findDetailById(orderId)
                .orElseThrow(() -> new BizException("领用单不存在"));
        if (order.getStatus() != OutboundStatus.PICKING) {
            throw new BizException("仅拣货中状态可完成拣货");
        }
        for (OutboundItem item : order.getItems()) {
            if (item.getLocation() == null) {
                throw new BizException("存在未分配库位的拣货项，请先开始拣货");
            }
            Long productId = item.getProduct().getId();
            Long locationId = item.getLocation().getId();
            Long batchId = item.getBatch() != null ? item.getBatch().getId() : null;

            Inventory inv;
            if (batchId != null) {
                inv = inventoryRepo.findByProductIdAndLocationIdAndBatchId(productId, locationId, batchId)
                        .orElseThrow(() -> new BizException("库存记录不存在"));
            } else {
                inv = inventoryRepo.findByProductIdAndLocationId(productId, locationId)
                        .orElseThrow(() -> new BizException("库存记录不存在"));
            }
            if (inv.getQty() < item.getRequestedQty()) {
                throw new BizException(String.format(
                        "库位 %s 商品 %s 库存不足，剩余 %d 件",
                        item.getLocation().getCode(), item.getProduct().getSku(), inv.getQty()));
            }
            int beforeQty = inv.getQty();
            int afterQty = beforeQty - item.getRequestedQty();
            inv.setQty(afterQty);
            inventoryRepo.save(inv);
            item.setPickedQty(item.getRequestedQty());
            // 熄灭灯光
            deviceService.lightOff(item.getLocation().getId());

            // 记录出库日志（含批次信息）
            Batch b = item.getBatch();
            inventoryLogService.record(
                    item.getProduct().getId(), item.getProduct().getSku(), item.getProduct().getName(),
                    item.getLocation().getId(), item.getLocation().getCode(),
                    "OUTBOUND", item.getRequestedQty(), beforeQty, afterQty,
                    "OUTBOUND", order.getOrderNo(),
                    currentOperator(), null,
                    b != null ? b.getItemKey() : null,
                    b != null ? b.getProductionDate() : null,
                    b != null ? b.getShelfLifeDays() : null,
                    b != null ? b.getManufacturer() : null
            );
        }
        order.setStatus(OutboundStatus.DONE);
        orderRepo.save(order);
        return Map.of("orderId", order.getId(), "done", true, "msg", "拣货完成，出库成功");
    }

    private OutboundOrderListItemVO toListItem(OutboundOrder o) {
        return new OutboundOrderListItemVO(
                o.getId(), o.getOrderNo(), o.getStatus().name(),
                o.getItems().size(), o.getCreatedAt()
        );
    }

    private OutboundOrderDetailVO toDetail(OutboundOrder o) {
        List<OutboundItemVO> itemVOs = o.getItems().stream()
                .map(it -> {
                    Long locId = it.getLocation() != null ? it.getLocation().getId() : null;
                    String locCode = it.getLocation() != null ? it.getLocation().getCode() : null;
                    String locArea = it.getLocation() != null ? it.getLocation().getArea() : null;
                    String deviceNo = it.getLocation() != null ? it.getLocation().getDeviceNo() : null;
                    int avail = 0;
                    Batch b = it.getBatch();
                    Long batchId = b != null ? b.getId() : null;
                    if (locId != null && batchId != null) {
                        avail = inventoryRepo.findByProductIdAndLocationIdAndBatchId(it.getProduct().getId(), locId, batchId)
                                .map(Inventory::getQty).orElse(0);
                    } else if (locId != null) {
                        avail = inventoryRepo.findByProductIdAndLocationId(it.getProduct().getId(), locId)
                                .map(Inventory::getQty).orElse(0);
                    }
                    return new OutboundItemVO(
                            it.getId(), it.getProduct().getId(), it.getProduct().getSku(),
                            it.getProduct().getName(), it.getRequestedQty(), it.getPickedQty(),
                            locId, locCode, locArea, deviceNo, avail,
                            batchId,
                            b != null ? b.getItemKey() : null,
                            b != null ? b.getProductionDate() : null,
                            b != null ? b.getShelfLifeDays() : null,
                            b != null ? b.getManufacturer() : null,
                            b != null ? b.getExpiryDate() : null
                    );
                }).collect(Collectors.toList());
        return new OutboundOrderDetailVO(o.getId(), o.getOrderNo(), o.getStatus().name(),
                itemVOs, o.getCreatedAt());
    }

    private String currentOperator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return String.valueOf(auth.getPrincipal());
    }
}
