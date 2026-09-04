package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.LocationDTO;
import com.wms.entity.Location;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LocationService {
    private final LocationRepository locationRepo;
    private final InventoryRepository inventoryRepo;

    public LocationService(LocationRepository locationRepo, InventoryRepository inventoryRepo) {
        this.locationRepo = locationRepo;
        this.inventoryRepo = inventoryRepo;
    }

    public Page<LocationDTO> page(String code, String area, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String c = StringUtils.hasText(code) ? code : "";
        String a = StringUtils.hasText(area) ? area : "";
        if (!c.isEmpty() || !a.isEmpty()) {
            return locationRepo.findByCodeContainingOrAreaContaining(c, a, pageable).map(this::toDTO);
        }
        return locationRepo.findAll(pageable).map(this::toDTO);
    }

    public LocationDTO getById(Long id) {
        return locationRepo.findById(id).map(this::toDTO)
                .orElseThrow(() -> new BizException("库位不存在"));
    }

    @Transactional
    public LocationDTO create(LocationDTO dto) {
        if (!StringUtils.hasText(dto.code())) throw new BizException("code不能为空");
        if (locationRepo.existsByCode(dto.code())) throw new BizException("code已存在");
        Location l = new Location(dto.code(), dto.area(), dto.type(), dto.remark());
        locationRepo.save(l);
        return toDTO(l);
    }

    @Transactional
    public LocationDTO update(Long id, LocationDTO dto) {
        Location l = locationRepo.findById(id).orElseThrow(() -> new BizException("库位不存在"));
        l.setArea(dto.area() == null ? l.getArea() : dto.area());
        l.setType(dto.type() == null ? l.getType() : dto.type());
        l.setRemark(dto.remark() == null ? l.getRemark() : dto.remark());
        locationRepo.save(l);
        return toDTO(l);
    }

    @Transactional
    public void delete(Long id) {
        Location l = locationRepo.findById(id).orElseThrow(() -> new BizException("库位不存在"));
        if (!inventoryRepo.findAllByLocationId(id).isEmpty()) {
            throw new BizException("库位[" + l.getCode() + "]存在库存记录，无法删除");
        }
        locationRepo.deleteById(id);
    }

    private LocationDTO toDTO(Location l) {
        return new LocationDTO(l.getId(), l.getCode(), l.getArea(), l.getType(), l.getRemark());
    }
}
