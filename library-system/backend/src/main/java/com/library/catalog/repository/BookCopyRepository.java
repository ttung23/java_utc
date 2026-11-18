package com.library.catalog.repository;

import com.library.catalog.entity.BookCopy;
import com.library.common.enums.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long>, JpaSpecificationExecutor<BookCopy> {

    List<BookCopy> findByBook_IdAndDeletedAtIsNullOrderByCopyNoAsc(Long bookId);

    List<BookCopy> findByBook_Id(Long bookId);

    Optional<BookCopy> findByIdAndDeletedAtIsNull(Long id);

    Optional<BookCopy> findByBarcodeAndDeletedAtIsNull(String barcode);

    long countByLocation_Id(Long locationId);

    long countByBook_IdAndDeletedAtIsNull(Long bookId);

    long countByBook_IdAndStatusAndDeletedAtIsNull(Long bookId, CopyStatus status);

    @Query("SELECT COALESCE(MAX(c.copyNo), 0) FROM BookCopy c WHERE c.book.id = :bookId")
    int findMaxCopyNo(@Param("bookId") Long bookId);

    @Query("""
            SELECT c.book.id, COUNT(c)
            FROM BookCopy c
            WHERE c.book.id IN :bookIds AND c.deletedAt IS NULL
            GROUP BY c.book.id
            """)
    List<Object[]> countTotalByBookIds(@Param("bookIds") Collection<Long> bookIds);

    @Query("""
            SELECT c.book.id, COUNT(c)
            FROM BookCopy c
            WHERE c.book.id IN :bookIds AND c.status = :status AND c.deletedAt IS NULL
            GROUP BY c.book.id
            """)
    List<Object[]> countByBookIdsAndStatus(
            @Param("bookIds") Collection<Long> bookIds,
            @Param("status") CopyStatus status
    );

    @Query("""
            SELECT c.status, COUNT(c)
            FROM BookCopy c
            WHERE c.book.id = :bookId AND c.deletedAt IS NULL
            GROUP BY c.status
            """)
    List<Object[]> countGroupByStatus(@Param("bookId") Long bookId);
}
