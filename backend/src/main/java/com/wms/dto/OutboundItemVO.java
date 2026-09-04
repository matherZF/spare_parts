package com.wms.dto;

import java.time.LocalDate;

// 出库明细项视图
public record OutboundItemVO(
        Long id,
        Long productId,
        String sku,
        String name,
        int requestedQty,
        int pickedQty,
        Long locationId,
        String locationCode,
        String locationArea,
        String deviceNo,
        int availableQty,
        // 批次信息
        Long batchId,
        String itemKey,
        LocalDate productionDate,
        Integer shelfLifeDays,
        String manufacturer,
        LocalDate expiryDate
) {}
