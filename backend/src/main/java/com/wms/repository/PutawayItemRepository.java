package com.wms.repository;

import com.wms.entity.PutawayItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PutawayItemRepository extends JpaRepository<PutawayItem, Long> {
}
