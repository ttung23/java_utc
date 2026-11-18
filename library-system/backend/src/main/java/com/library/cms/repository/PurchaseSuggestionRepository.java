package com.library.cms.repository;

import com.library.cms.entity.PurchaseSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseSuggestionRepository extends JpaRepository<PurchaseSuggestion, Long> {
    Optional<PurchaseSuggestion> findByIdAndDeletedAtIsNull(Long id);

    Page<PurchaseSuggestion> findByDeletedAtIsNull(Pageable pageable);
}
