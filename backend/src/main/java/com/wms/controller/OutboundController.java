package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.AvailableStockVO;
import com.wms.dto.OutboundCreateReq;
import com.wms.dto.OutboundItemVO;
import com.wms.dto.OutboundOrderDetailVO;
import com.wms.dto.OutboundOrderListItemVO;
import com.wms.service.OutboundService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outbound")
public class OutboundController {
    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @GetMapping
    public Result<Page<OutboundOrderListItemVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(outboundService.page(status, page, size));
    }

    @GetMapping("/pending")
    public Result<List<OutboundOrderListItemVO>> pending() {
        return Result.ok(outboundService.pendingList());
    }

    @GetMapping("/{id}")
    public Result<OutboundOrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(outboundService.detail(id));
    }

    @PostMapping
    public Result<OutboundOrderDetailVO> create(@Valid @RequestBody OutboundCreateReq req) {
        return Result.ok(outboundService.create(req));
    }

    /** 查询某商品的可用库存（含批次详情），用于人工选择库位/批次 */
    @GetMapping("/available-stock")
    public Result<List<AvailableStockVO>> availableStock(@RequestParam Long productId) {
        return Result.ok(outboundService.listAvailableInventory(productId));
    }

    /** 人工为出库项分配库位+批次 */
    @PostMapping("/{orderId}/items/{itemId}/assign")
    public Result<OutboundItemVO> assignLocation(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Long locationId,
            @RequestParam(required = false) Long batchId) {
        return Result.ok(outboundService.assignLocation(orderId, itemId, locationId, batchId));
    }

    /** 开始拣货：分配库位 + 触发灯光设备 */
    @PostMapping("/{id}/start")
    public Result<Map<String, Object>> start(@PathVariable Long id) {
        return Result.ok(outboundService.startPicking(id));
    }

    /** 拣货完成：扣减库存 */
    @PostMapping("/{id}/complete")
    public Result<Map<String, Object>> complete(@PathVariable Long id) {
        return Result.ok(outboundService.completePicking(id));
    }
}
