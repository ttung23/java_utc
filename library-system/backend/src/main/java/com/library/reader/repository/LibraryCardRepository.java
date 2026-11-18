package com.library.reader.repository;

import com.library.common.enums.CardStatus;
import com.library.reader.entity.LibraryCard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibraryCardRepository extends JpaRepository<LibraryCard, Long>, JpaSpecificationExecutor<LibraryCard> {

    Optional<LibraryCard> findByIdAndDeletedAtIsNull(Long id);

    Optional<LibraryCard> findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(
            Long readerId, CardStatus status);

    List<LibraryCard> findByReader_IdAndDeletedAtIsNullOrderByIssuedAtDesc(Long readerId);

    boolean existsByReader_IdAndStatusAndDeletedAtIsNull(Long readerId, CardStatus status);

    @Query("""
            SELECT c FROM LibraryCard c
            WHERE c.deletedAt IS NULL
              AND (LOWER(c.cardNumber) = LOWER(:value) OR LOWER(c.barcode) = LOWER(:value))
            """)
    Optional<LibraryCard> findByCardNumberOrBarcode(@Param("value") String value);

    @Query("""
            SELECT c.cardNumber FROM LibraryCard c
            WHERE c.cardNumber LIKE CONCAT(:prefix, '%')
            ORDER BY c.cardNumber DESC
            """)
    List<String> findCardNumbersByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
