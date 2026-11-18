package com.library.fine.repository;

import com.library.common.enums.FineStatus;
import com.library.fine.entity.Fine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    Optional<Fine> findByIdAndDeletedAtIsNull(Long id);

    Page<Fine> findByDeletedAtIsNull(Pageable pageable);

    Page<Fine> findByReader_IdAndDeletedAtIsNull(Long readerId, Pageable pageable);

    Page<Fine> findByReader_IdAndStatusInAndDeletedAtIsNull(Long readerId, Collection<FineStatus> statuses, Pageable pageable);

    List<Fine> findByIdInAndDeletedAtIsNull(Collection<Long> ids);

    @Query("""
            SELECT COALESCE(SUM(f.amount - f.paidAmount), 0)
            FROM Fine f
            WHERE f.reader.id = :readerId
              AND f.deletedAt IS NULL
              AND f.status IN (
                  com.library.common.enums.FineStatus.UNPAID,
                  com.library.common.enums.FineStatus.PARTIAL
              )
            """)
    BigDecimal sumUnpaidByReader(@Param("readerId") Long readerId);

    @Query("""
            SELECT COALESCE(SUM(f.amount - f.paidAmount), 0)
            FROM Fine f
            WHERE f.deletedAt IS NULL
              AND f.status IN (
                  com.library.common.enums.FineStatus.UNPAID,
                  com.library.common.enums.FineStatus.PARTIAL
              )
            """)
    BigDecimal sumAllUnpaid();
}
