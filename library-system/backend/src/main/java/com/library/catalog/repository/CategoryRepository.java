package com.library.catalog.repository;

import com.library.catalog.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndDeletedAtIsNull(Long id);

    Optional<Category> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, Long id);

    boolean existsByParent_IdAndDeletedAtIsNull(Long parentId);

    Page<Category> findByDeletedAtIsNull(Pageable pageable);

    Page<Category> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);

    List<Category> findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAscNameAsc();

    List<Category> findByDeletedAtIsNullOrderBySortOrderAscNameAsc();
}
