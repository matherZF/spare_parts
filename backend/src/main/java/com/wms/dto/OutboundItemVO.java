package com.wms.dto;

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
        int availableQty
) {}
