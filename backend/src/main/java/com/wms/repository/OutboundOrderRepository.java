package com.wms.repository;

import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
    Optional<OutboundOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix);

    @EntityGraph(attributePaths = {"items"})
    @Query("select o from OutboundOrder o where (:status is null or o.status = :status) " +
            "order by o.id desc")
    Page<OutboundOrder> findByStatus(@Param("status") OutboundStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("select o from OutboundOrder o where o.status = :status order by o.id desc")
    List<OutboundOrder> findPendingList(@Param("status") OutboundStatus status);

    @EntityGraph(attributePaths = {"equipment", "items", "items.product", "items.location", "items.batch"})
    @Query("select o from OutboundOrder o where o.id = :id")
    Optional<OutboundOrder> findDetailById(@Param("id") Long id);
}
