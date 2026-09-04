package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.InventoryDetailVO;
import com.wms.dto.InventorySummaryVO;
import com.wms.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/summary")
    public Result<List<InventorySummaryVO>> summary(@RequestParam(required = false) String sku) {
        return Result.ok(inventoryService.summary(sku));
    }

    @GetMapping("/details")
    public Result<List<InventoryDetailVO>> details(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String locationCode) {
        return Result.ok(inventoryService.details(sku, locationCode));
    }
}
