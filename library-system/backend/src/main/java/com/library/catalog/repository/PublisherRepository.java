package com.library.catalog.repository;

import com.library.catalog.entity.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    Optional<Publisher> findByIdAndDeletedAtIsNull(Long id);

    Page<Publisher> findByDeletedAtIsNull(Pageable pageable);

    Page<Publisher> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);
}
