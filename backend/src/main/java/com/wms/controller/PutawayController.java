package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.PutawayConfirmReq;
import com.wms.service.PutawayService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/putaway")
public class PutawayController {
    private final PutawayService putawayService;

    public PutawayController(PutawayService putawayService) {
        this.putawayService = putawayService;
    }

    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody PutawayConfirmReq req) {
        return Result.ok(putawayService.confirm(req));
    }
}
