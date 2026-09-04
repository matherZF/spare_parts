package com.wms.repository;

import com.wms.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Page<Location> findByCodeContainingOrAreaContaining(String code, String area, Pageable pageable);
    boolean existsByCode(String code);
    List<Location> findByCodeContaining(String code);
}
