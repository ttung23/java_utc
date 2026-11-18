package com.library.borrow.repository;

import com.library.borrow.entity.BorrowSlipItem;
import com.library.common.enums.BorrowItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BorrowSlipItemRepository extends JpaRepository<BorrowSlipItem, Long> {

    List<BorrowSlipItem> findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(Long slipId);

    @Query("""
            SELECT COUNT(i) FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
            """)
    long countActiveBorrowingByReader(@Param("readerId") Long readerId);

    @Query("""
            SELECT COUNT(i) FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.deletedAt IS NULL
              AND i.status IN (
                  com.library.common.enums.BorrowItemStatus.REQUESTED,
                  com.library.common.enums.BorrowItemStatus.HOLD
              )
              AND i.slip.status IN (
                  com.library.common.enums.BorrowSlipStatus.PENDING,
                  com.library.common.enums.BorrowSlipStatus.APPROVED
              )
            """)
    long countPendingOnlineByReader(@Param("readerId") Long readerId);

    @Query("""
            SELECT COUNT(i) FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
              AND i.dueDate < :today
            """)
    long countOverdueByReader(@Param("readerId") Long readerId, @Param("today") LocalDate today);

    @Query("""
            SELECT DISTINCT i.book.id FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
            """)
    List<Long> findBorrowedBookIdsByReader(@Param("readerId") Long readerId);

    @Query("""
            SELECT DISTINCT i.book.id FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.deletedAt IS NULL
              AND i.status IN (
                  com.library.common.enums.BorrowItemStatus.REQUESTED,
                  com.library.common.enums.BorrowItemStatus.HOLD
              )
              AND i.slip.status IN (
                  com.library.common.enums.BorrowSlipStatus.PENDING,
                  com.library.common.enums.BorrowSlipStatus.APPROVED
              )
            """)
    List<Long> findPendingOnlineBookIdsByReader(@Param("readerId") Long readerId);

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM BorrowSlipItem i
            WHERE i.slip.reader.id = :readerId
              AND i.copy.id = :copyId
              AND i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.HOLD
            """)
    boolean existsHoldForReaderAndCopy(@Param("readerId") Long readerId, @Param("copyId") Long copyId);

    Optional<BorrowSlipItem> findFirstByCopy_IdAndStatusAndDeletedAtIsNull(
            Long copyId, BorrowItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM BorrowSlipItem i WHERE i.id = :id AND i.deletedAt IS NULL")
    Optional<BorrowSlipItem> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT i FROM BorrowSlipItem i
            JOIN FETCH i.slip s
            JOIN FETCH s.reader r
            JOIN FETCH r.user
            JOIN FETCH i.book
            LEFT JOIN FETCH i.copy
            WHERE i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
            ORDER BY i.dueDate ASC
            """)
    List<BorrowSlipItem> findAllBorrowingItems();

    @Query("""
            SELECT i FROM BorrowSlipItem i
            JOIN FETCH i.slip s
            JOIN FETCH s.reader r
            JOIN FETCH r.user
            JOIN FETCH i.book
            LEFT JOIN FETCH i.copy
            WHERE i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
              AND i.dueDate < :today
            ORDER BY i.dueDate ASC
            """)
    List<BorrowSlipItem> findOverdueItems(@Param("today") LocalDate today);

    @Query("""
            SELECT i FROM BorrowSlipItem i
            JOIN FETCH i.slip s
            JOIN FETCH s.reader r
            JOIN FETCH r.user
            JOIN FETCH i.book
            LEFT JOIN FETCH i.copy
            WHERE i.deletedAt IS NULL
              AND i.status = com.library.common.enums.BorrowItemStatus.BORROWING
              AND i.dueDate BETWEEN :from AND :to
            ORDER BY i.dueDate ASC
            """)
    List<BorrowSlipItem> findDueSoonItems(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(i) FROM BorrowSlipItem i
            WHERE i.slip.id = :slipId
              AND i.deletedAt IS NULL
              AND i.status IN :statuses
            """)
    long countBySlipAndStatuses(@Param("slipId") Long slipId, @Param("statuses") Collection<BorrowItemStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM BorrowSlipItem i
            WHERE i.slip.reader.user.id = :userId
              AND i.book.id = :bookId
              AND i.deletedAt IS NULL
              AND i.status IN (
                  com.library.common.enums.BorrowItemStatus.BORROWING,
                  com.library.common.enums.BorrowItemStatus.RETURNED,
                  com.library.common.enums.BorrowItemStatus.LOST,
                  com.library.common.enums.BorrowItemStatus.DAMAGED
              )
            """)
    boolean existsBorrowHistoryByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
