package com.library.inventory.repository;

import com.library.inventory.entity.InventoryAuditItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryAuditItemRepository extends JpaRepository<InventoryAuditItem, Long> {
    List<InventoryAuditItem> findByAudit_Id(Long auditId);

    Optional<InventoryAuditItem> findByAudit_IdAndCopy_Id(Long auditId, Long copyId);
}
