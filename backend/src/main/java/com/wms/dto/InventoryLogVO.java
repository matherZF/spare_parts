package com.wms.dto;

import java.time.Instant;

/**
 * 库存变动日志列表项
 */
public record InventoryLogVO(
        Long id,
        Long productId,
        String sku,
        String productName,
        Long locationId,
        String locationCode,
        String changeType,
        int changeQty,
        int beforeQty,
        int afterQty,
        String refType,
        String refNo,
        String operator,
        String remark,
        Instant createdAt
) {}
