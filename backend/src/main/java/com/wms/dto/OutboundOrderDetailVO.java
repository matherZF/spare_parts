package com.wms.dto;

import java.time.Instant;
import java.util.List;

// 出库单详情视图
public record OutboundOrderDetailVO(
        Long id,
        String orderNo,
        String status,
        List<OutboundItemVO> items,
        Instant createdAt
) {}
