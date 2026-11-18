package com.library.cms.repository;

import com.library.cms.entity.News;
import com.library.common.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findByIdAndDeletedAtIsNull(Long id);

    Optional<News> findBySlugAndDeletedAtIsNull(String slug);

    Page<News> findByDeletedAtIsNull(Pageable pageable);

    List<News> findByStatusAndDeletedAtIsNullOrderByPinnedDescPublishedAtDesc(NewsStatus status);

    boolean existsBySlug(String slug);
}
