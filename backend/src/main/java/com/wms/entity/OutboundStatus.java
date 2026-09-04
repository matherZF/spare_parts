package com.wms.entity;

// 出库单状态：待拣货 / 拣货中 / 已完成
public enum OutboundStatus {
    PENDING,    // 待拣货
    PICKING,    // 拣货中
    DONE        // 已完成
}
