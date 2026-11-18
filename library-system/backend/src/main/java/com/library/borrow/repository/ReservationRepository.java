package com.library.borrow.repository;

import com.library.borrow.entity.Reservation;
import com.library.common.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByIdAndDeletedAtIsNull(Long id);

    Page<Reservation> findByDeletedAtIsNull(Pageable pageable);

    Page<Reservation> findByReader_IdAndDeletedAtIsNull(Long readerId, Pageable pageable);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.book.id = :bookId
              AND r.deletedAt IS NULL
              AND r.status = com.library.common.enums.ReservationStatus.WAITING
            ORDER BY r.createdAt ASC
            """)
    List<Reservation> findWaitingQueue(@Param("bookId") Long bookId);

    Optional<Reservation> findFirstByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            Long bookId, ReservationStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.book.id = :bookId
              AND r.deletedAt IS NULL
              AND r.status IN (
                  com.library.common.enums.ReservationStatus.WAITING,
                  com.library.common.enums.ReservationStatus.READY
              )
            """)
    boolean existsActiveQueueForBook(@Param("bookId") Long bookId);

    List<Reservation> findByReader_IdAndBook_IdAndStatusInAndDeletedAtIsNull(
            Long readerId, Long bookId, List<ReservationStatus> statuses);

    long countByReader_IdAndStatusInAndDeletedAtIsNull(Long readerId, List<ReservationStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            WHERE r.deletedAt IS NULL
              AND r.status = com.library.common.enums.ReservationStatus.READY
              AND r.reader.id = :readerId
              AND r.assignedCopy.id = :copyId
            """)
    boolean existsReadyHoldForReaderAndCopy(@Param("readerId") Long readerId, @Param("copyId") Long copyId);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.deletedAt IS NULL
              AND r.status = com.library.common.enums.ReservationStatus.READY
              AND r.readyUntil < :now
            """)
    List<Reservation> findExpiredReady(@Param("now") Instant now);
}
