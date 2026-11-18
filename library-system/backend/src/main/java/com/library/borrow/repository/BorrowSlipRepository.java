package com.library.borrow.repository;

import com.library.borrow.entity.BorrowSlip;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowSlipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip, Long>, JpaSpecificationExecutor<BorrowSlip> {

    Optional<BorrowSlip> findByIdAndDeletedAtIsNull(Long id);

    Optional<BorrowSlip> findBySlipCodeAndDeletedAtIsNull(String slipCode);

    Page<BorrowSlip> findByReader_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long readerId, Pageable pageable);

    @Query("""
            SELECT COUNT(s) FROM BorrowSlip s
            WHERE s.slipCode LIKE :prefix AND s.deletedAt IS NULL
            """)
    long countBySlipCodePrefix(@Param("prefix") String prefix);

    @Query("""
            SELECT s FROM BorrowSlip s
            WHERE s.deletedAt IS NULL
              AND s.status = com.library.common.enums.BorrowSlipStatus.APPROVED
              AND s.pickupDeadline < :now
            """)
    List<BorrowSlip> findExpiredUnclaimed(@Param("now") Instant now);

    List<BorrowSlip> findByStatusAndChannelAndDeletedAtIsNull(BorrowSlipStatus status, BorrowChannel channel);
}
