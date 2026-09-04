package com.wms.dto;

import java.time.Instant;

public record InventoryDetailVO(
        Long id,
        Long productId,
        String sku,
        String productName,
        Long locationId,
        String locationCode,
        String area,
        Integer qty,
        Instant updatedAt
) {
}
