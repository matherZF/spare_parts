package com.wms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 出库单创建请求：包含多个拣货项
public record OutboundCreateReq(
        @NotEmpty List<OutboundItemCreateReq> items
) {}
