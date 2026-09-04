package com.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PutawayOrderCreateReq(
        @NotNull(message = "productId不能为空") Long productId,
        @NotNull(message = "planQty不能为空")
        @Min(value = 1, message = "planQty必须大于0") Integer planQty
) {
}
