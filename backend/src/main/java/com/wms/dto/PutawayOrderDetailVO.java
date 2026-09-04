package com.wms.dto;

import java.time.Instant;
import java.util.List;

public record PutawayOrderDetailVO(
        Long id,
        String orderNo,
        Long productId,
        String sku,
        String productName,
        Integer planQty,
        Integer putQty,
        String status,
        Integer progress,
        Integer remainingQty,
        List<PutawayItemVO> items,
        Instant createdAt
) {
}
