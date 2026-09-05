package com.wms.controller;
import com.wms.common.Result; import com.wms.service.EquipmentService; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/equipments") public class EquipmentController { private final EquipmentService s; public EquipmentController(EquipmentService s){this.s=s;}
 @GetMapping public Result<List<Map<String,Object>>> list(@RequestParam(required=false)String keyword){return Result.ok(s.list(keyword));}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") public Result<Map<String,Object>> create(@RequestBody Map<String,Object> r){return Result.ok(s.save(null,r));}
 @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public Result<Map<String,Object>> update(@PathVariable Long id,@RequestBody Map<String,Object>r){return Result.ok(s.save(id,r));}
 @GetMapping("/{id}/parts") public Result<List<Map<String,Object>>> parts(@PathVariable Long id){return Result.ok(s.parts(id));}
 @PostMapping("/{id}/parts") @PreAuthorize("hasRole('ADMIN')") public Result<Map<String,Object>> bind(@PathVariable Long id,@RequestBody Map<String,Object>r){return Result.ok(s.bindPart(id,r));}
}
