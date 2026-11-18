package com.library.catalog.repository;

import com.library.catalog.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByIdAndDeletedAtIsNull(Long id);

    Page<Author> findByDeletedAtIsNull(Pageable pageable);

    Page<Author> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);
}
