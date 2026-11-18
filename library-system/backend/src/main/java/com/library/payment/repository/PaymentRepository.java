package com.library.payment.repository;

import com.library.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndDeletedAtIsNull(Long id);

    Page<Payment> findByDeletedAtIsNull(Pageable pageable);

    Page<Payment> findByReader_IdAndDeletedAtIsNull(Long readerId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.receiptCode LIKE :prefix AND p.deletedAt IS NULL")
    long countByReceiptPrefix(@Param("prefix") String prefix);
}
