package com.wms.controller;

import com.wms.common.Result;
import com.wms.dto.LocationDTO;
import com.wms.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public Result<Page<LocationDTO>> page(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(locationService.page(code, area, page, size));
    }

    @GetMapping("/{id}")
    public Result<LocationDTO> get(@PathVariable Long id) {
        return Result.ok(locationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LocationDTO> create(@Valid @RequestBody LocationDTO dto) {
        return Result.ok(locationService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LocationDTO> update(@PathVariable Long id, @RequestBody LocationDTO dto) {
        return Result.ok(locationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return Result.ok();
    }
}
