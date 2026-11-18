package com.library.inventory.repository;

import com.library.inventory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByIdAndDeletedAtIsNull(Long id);

    Page<Supplier> findByDeletedAtIsNull(Pageable pageable);

    Page<Supplier> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);
}
