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

    public OutboundService(OutboundOrderRepository orderRepo,
                           ProductRepository productRepo,
                           InventoryRepository inventoryRepo,
                           DeviceService deviceService) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.inventoryRepo = inventoryRepo;
        this.deviceService = deviceService;
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

    public OutboundOrderDetailVO detail(Long id) {
        return orderRepo.findDetailById(id).map(this::toDetail)
                .orElseThrow(() -> new BizException("领用单不存在"));
    }

    /**
     * 开始拣货：为每个商品项分配库存库位，并点亮对应库位灯光设备。
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
            // 找到该商品有库存的库位（按库存量降序，取第一个）
            List<Inventory> stocks = inventoryRepo.findAllByProductId(item.getProduct().getId());
            Inventory stock = stocks.stream()
                    .filter(s -> s.getQty() > 0)
                    .max(Comparator.comparingInt(Inventory::getQty))
                    .orElse(null);
            if (stock == null) {
                throw new BizException(String.format("商品 %s 无可用库存", item.getProduct().getSku()));
            }
            item.setLocation(stock.getLocation());
            // 预留接口：点亮库位灯光设备
            String deviceNo = deviceService.lightUp(stock.getLocation().getId());
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
            Inventory inv = inventoryRepo
                    .findByProductIdAndLocationId(item.getProduct().getId(), item.getLocation().getId())
                    .orElseThrow(() -> new BizException("库存记录不存在"));
            if (inv.getQty() < item.getRequestedQty()) {
                throw new BizException(String.format(
                        "库位 %s 商品 %s 库存不足，剩余 %d 件",
                        item.getLocation().getCode(), item.getProduct().getSku(), inv.getQty()));
            }
            inv.setQty(inv.getQty() - item.getRequestedQty());
            inventoryRepo.save(inv);
            item.setPickedQty(item.getRequestedQty());
            // 熄灭灯光
            deviceService.lightOff(item.getLocation().getId());
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
                    if (locId != null) {
                        avail = inventoryRepo.findByProductIdAndLocationId(it.getProduct().getId(), locId)
                                .map(Inventory::getQty).orElse(0);
                    }
                    return new OutboundItemVO(
                            it.getId(), it.getProduct().getId(), it.getProduct().getSku(),
                            it.getProduct().getName(), it.getRequestedQty(), it.getPickedQty(),
                            locId, locCode, locArea, deviceNo, avail
                    );
                }).collect(Collectors.toList());
        return new OutboundOrderDetailVO(o.getId(), o.getOrderNo(), o.getStatus().name(),
                itemVOs, o.getCreatedAt());
    }
}
