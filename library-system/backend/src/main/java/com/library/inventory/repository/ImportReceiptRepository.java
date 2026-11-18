package com.library.inventory.repository;

import com.library.inventory.entity.ImportReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long> {
    Optional<ImportReceipt> findByIdAndDeletedAtIsNull(Long id);

    Page<ImportReceipt> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(r) FROM ImportReceipt r WHERE r.receiptCode LIKE :prefix AND r.deletedAt IS NULL")
    long countByCodePrefix(@Param("prefix") String prefix);
}
