package com.library.cms.repository;

import com.library.cms.entity.Review;
import com.library.common.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(Long bookId, ReviewStatus status);

    List<Review> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Review> findByUser_IdAndBook_IdAndDeletedAtIsNull(Long userId, Long bookId);

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);
}
