package com.library.inventory.repository;

import com.library.inventory.entity.InventoryAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryAuditRepository extends JpaRepository<InventoryAudit, Long> {
    Optional<InventoryAudit> findByIdAndDeletedAtIsNull(Long id);

    Page<InventoryAudit> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(a) FROM InventoryAudit a WHERE a.code LIKE :prefix AND a.deletedAt IS NULL")
    long countByCodePrefix(@Param("prefix") String prefix);
}
