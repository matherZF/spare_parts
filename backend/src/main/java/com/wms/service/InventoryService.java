package com.wms.service;

import com.wms.dto.InventoryDetailVO;
import com.wms.dto.InventorySummaryVO;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepo;

    public InventoryService(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    public List<InventorySummaryVO> summary(String skuKeyword) {
        List<Inventory> list = inventoryRepo.findAllWithGraph();
        String kw = StringUtils.hasText(skuKeyword) ? skuKeyword.trim().toLowerCase() : "";
        Map<Long, InventorySummaryVO> map = new LinkedHashMap<>();
        for (Inventory inv : list) {
            var p = inv.getProduct();
            if (!kw.isEmpty()) {
                boolean m1 = p.getSku() != null && p.getSku().toLowerCase().contains(kw);
                boolean m2 = p.getName() != null && p.getName().toLowerCase().contains(kw);
                if (!m1 && !m2) continue;
            }
            InventorySummaryVO s = map.get(p.getId());
            int addQty = inv.getQty();
            if (s == null) {
                s = new InventorySummaryVO(p.getId(), p.getSku(), p.getName(), p.getSpec(), p.getUnit(), addQty);
            } else {
                s = new InventorySummaryVO(s.productId(), s.sku(), s.name(), s.spec(), s.unit(),
                        s.totalQty() + addQty);
            }
            map.put(p.getId(), s);
        }
        return new ArrayList<>(map.values());
    }

    public List<InventoryDetailVO> details(String sku, String locationCode) {
        List<Inventory> list = inventoryRepo.findAllWithGraph();
        String kw = StringUtils.hasText(sku) ? sku.trim().toLowerCase() : "";
        String locKw = StringUtils.hasText(locationCode) ? locationCode.trim().toLowerCase() : "";
        return list.stream().filter(inv -> {
            boolean ok1 = true, ok2 = true;
            if (!kw.isEmpty()) {
                boolean m1 = inv.getProduct().getSku() != null && inv.getProduct().getSku().toLowerCase().contains(kw);
                boolean m2 = inv.getProduct().getName() != null && inv.getProduct().getName().toLowerCase().contains(kw);
                ok1 = m1 || m2;
            }
            if (!locKw.isEmpty()) {
                ok2 = inv.getLocation().getCode() != null && inv.getLocation().getCode().toLowerCase().contains(locKw);
            }
            return ok1 && ok2;
        }).sorted(Comparator.comparing((Inventory a) -> a.getProduct().getSku())
                .thenComparing(a -> a.getLocation().getCode()))
                .map(inv -> new InventoryDetailVO(
                        inv.getId(),
                        inv.getProduct().getId(),
                        inv.getProduct().getSku(),
                        inv.getProduct().getName(),
                        inv.getLocation().getId(),
                        inv.getLocation().getCode(),
                        inv.getLocation().getArea(),
                        inv.getQty(),
                        inv.getUpdatedAt()
                )).collect(Collectors.toList());
    }
}
