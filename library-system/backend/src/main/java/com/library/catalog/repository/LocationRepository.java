package com.library.catalog.repository;

import com.library.catalog.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByIdAndDeletedAtIsNull(Long id);

    Optional<Location> findByCode(String code);

    Optional<Location> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, Long id);

    Page<Location> findByDeletedAtIsNull(Pageable pageable);

    Page<Location> findByDeletedAtIsNullAndCodeContainingIgnoreCase(String code, Pageable pageable);
}
