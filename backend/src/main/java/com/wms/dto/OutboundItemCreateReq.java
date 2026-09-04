package com.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 出库单项创建请求
public record OutboundItemCreateReq(
        @NotNull Long productId,
        @NotNull @Min(1) Integer requestedQty
) {}
