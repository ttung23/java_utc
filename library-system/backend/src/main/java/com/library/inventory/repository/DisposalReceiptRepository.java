package com.library.inventory.repository;

import com.library.inventory.entity.DisposalReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DisposalReceiptRepository extends JpaRepository<DisposalReceipt, Long> {
    Optional<DisposalReceipt> findByIdAndDeletedAtIsNull(Long id);

    Page<DisposalReceipt> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT COUNT(d) FROM DisposalReceipt d WHERE d.code LIKE :prefix AND d.deletedAt IS NULL")
    long countByCodePrefix(@Param("prefix") String prefix);
}
