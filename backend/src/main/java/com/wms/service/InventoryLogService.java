package com.wms.service;

import com.wms.dto.InventoryLogVO;
import com.wms.entity.InventoryLog;
import com.wms.repository.InventoryLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InventoryLogService {

    private final InventoryLogRepository logRepo;

    public InventoryLogService(InventoryLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    /**
     * 记录一条库存变动日志
     */
    public InventoryLog record(Long productId, String sku, String productName,
                               Long locationId, String locationCode,
                               String changeType, int changeQty,
                               int beforeQty, int afterQty,
                               String refType, String refNo,
                               String operator, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProductId(productId);
        log.setSku(sku);
        log.setProductName(productName);
        log.setLocationId(locationId);
        log.setLocationCode(locationCode);
        log.setChangeType(changeType);
        log.setChangeQty(changeQty);
        log.setBeforeQty(beforeQty);
        log.setAfterQty(afterQty);
        log.setRefType(refType);
        log.setRefNo(refNo);
        log.setOperator(operator);
        log.setRemark(remark);
        return logRepo.save(log);
    }

    /**
     * 分页查询库存日志
     */
    public Page<InventoryLogVO> page(String sku, String locationCode, String changeType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String skuKw = StringUtils.hasText(sku) ? sku.trim() : null;
        String locKw = StringUtils.hasText(locationCode) ? locationCode.trim() : null;
        String type = StringUtils.hasText(changeType) ? changeType.trim().toUpperCase() : null;
        return logRepo.search(skuKw, locKw, type, pageable).map(this::toVO);
    }

    private InventoryLogVO toVO(InventoryLog l) {
        return new InventoryLogVO(
                l.getId(), l.getProductId(), l.getSku(), l.getProductName(),
                l.getLocationId(), l.getLocationCode(),
                l.getChangeType(), l.getChangeQty(), l.getBeforeQty(), l.getAfterQty(),
                l.getRefType(), l.getRefNo(), l.getOperator(), l.getRemark(), l.getCreatedAt()
        );
    }
}
