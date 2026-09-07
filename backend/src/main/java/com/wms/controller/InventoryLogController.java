package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.InventoryLogVO;
import com.wms.service.InventoryLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-logs")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    public InventoryLogController(InventoryLogService inventoryLogService) {
        this.inventoryLogService = inventoryLogService;
    }

    @GetMapping
    public Result<Page<InventoryLogVO>> page(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(inventoryLogService.page(sku, locationCode, changeType, page, size));
    }
}
