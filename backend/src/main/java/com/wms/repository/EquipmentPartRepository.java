package com.wms.repository;
import com.wms.entity.EquipmentPart; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface EquipmentPartRepository extends JpaRepository<EquipmentPart,Long>{
 @EntityGraph(attributePaths={"equipment","product"}) List<EquipmentPart> findByEquipmentId(Long equipmentId);
 @EntityGraph(attributePaths={"equipment","product"}) List<EquipmentPart> findAll();
 Optional<EquipmentPart> findByEquipmentIdAndProductId(Long equipmentId,Long productId);
}
