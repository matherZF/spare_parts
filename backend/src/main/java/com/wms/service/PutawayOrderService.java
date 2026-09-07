package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.*;
import com.wms.entity.Batch;
import com.wms.entity.Product;
import com.wms.entity.PutawayItem;
import com.wms.entity.PutawayOrder;
import com.wms.entity.PutawayStatus;
import com.wms.repository.BatchRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.PutawayOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PutawayOrderService {
    private final PutawayOrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final BatchRepository batchRepo;

    public PutawayOrderService(PutawayOrderRepository orderRepo,
                               ProductRepository productRepo,
                               BatchRepository batchRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.batchRepo = batchRepo;
    }

    public synchronized String generateOrderNo() {
        String prefix = "PA" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
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

    private synchronized String generateItemKey(Product product) {
        String prefix = "B" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        // 简化：基于时间戳生成唯一批次号
        return prefix + System.currentTimeMillis() % 100000;
    }

    @Transactional
    public PutawayOrderDetailVO create(PutawayOrderCreateReq req) {
        Product product = productRepo.findById(req.productId())
                .orElseThrow(() -> new BizException("商品不存在"));
        if (req.planQty() == null || req.planQty() <= 0) {
            throw new BizException("planQty必须大于0");
        }

        // 1. 创建批次
        Batch batch = new Batch();
        String itemKey = StringUtils.hasText(req.itemKey()) ? req.itemKey().trim() : generateItemKey(product);
        if (batchRepo.existsByItemKey(itemKey)) {
            throw new BizException("批次号已存在：" + itemKey);
        }
        batch.setItemKey(itemKey);
        batch.setProductId(product.getId());
        batch.setSku(product.getSku());
        batch.setProductName(product.getName());
        batch.setProductionDate(req.productionDate());
        batch.setShelfLifeDays(req.shelfLifeDays());
        batch.setManufacturer(StringUtils.hasText(req.manufacturer()) ? req.manufacturer().trim() : null);
        batch = batchRepo.save(batch);

        // 2. 创建入库单并关联批次
        PutawayOrder order = new PutawayOrder();
        order.setOrderNo(generateOrderNo());
        order.setProduct(product);
        order.setBatch(batch);
        order.setPlanQty(req.planQty());
        order.setPutQty(0);
        order.setStatus(PutawayStatus.PENDING);
        orderRepo.save(order);
        return toDetail(order);
    }

    public Page<PutawayOrderListItemVO> page(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        PutawayStatus s = null;
        if (StringUtils.hasText(status)) {
            try { s = PutawayStatus.valueOf(status.trim().toUpperCase()); }
            catch (Exception e) { throw new BizException("状态参数无效"); }
        }
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        return orderRepo.findByStatusAndKeyword(s, kw, pageable).map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public PutawayOrderDetailVO detail(Long id) {
        return orderRepo.findDetailById(id).map(this::toDetail)
                .orElseThrow(() -> new BizException("上架单不存在"));
    }

    public List<PutawayOrderListItemVO> pendingList() {
        return orderRepo.findByStatusWithProduct(PutawayStatus.PENDING).stream()
                .sorted((a,b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    private PutawayOrderListItemVO toListItem(PutawayOrder o) {
        int progress = o.getPlanQty() == 0 ? 0 : (int) Math.round(o.getPutQty() * 100.0 / o.getPlanQty());
        return new PutawayOrderListItemVO(
                o.getId(), o.getOrderNo(),
                o.getProduct().getId(), o.getProduct().getSku(), o.getProduct().getName(),
                o.getPlanQty(), o.getPutQty(), o.getStatus().name(),
                progress, o.getCreatedAt()
        );
    }

    PutawayOrderDetailVO toDetail(PutawayOrder o) {
        int progress = o.getPlanQty() == 0 ? 0 : (int) Math.round(o.getPutQty() * 100.0 / o.getPlanQty());
        List<PutawayItemVO> itemVO = o.getItems().stream()
                .sorted((a,b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toItemVO)
                .collect(Collectors.toList());

        // 批次信息
        Batch b = o.getBatch();
        String itemKey = b != null ? b.getItemKey() : null;
        LocalDate productionDate = b != null ? b.getProductionDate() : null;
        Integer shelfLifeDays = b != null ? b.getShelfLifeDays() : null;
        String manufacturer = b != null ? b.getManufacturer() : null;
        LocalDate expiryDate = b != null ? b.getExpiryDate() : null;

        return new PutawayOrderDetailVO(
                o.getId(), o.getOrderNo(),
                o.getProduct().getId(), o.getProduct().getSku(), o.getProduct().getName(),
                o.getPlanQty(), o.getPutQty(), o.getStatus().name(),
                progress, Math.max(0, o.getPlanQty() - o.getPutQty()),
                itemVO, o.getCreatedAt(),
                itemKey, productionDate, shelfLifeDays, manufacturer, expiryDate
        );
    }

    private PutawayItemVO toItemVO(PutawayItem it) {
        return new PutawayItemVO(
                it.getId(),
                it.getLocation().getId(),
                it.getLocation().getCode(),
                it.getLocation().getArea(),
                it.getQty(),
                it.getCreatedAt()
        );
    }
}
