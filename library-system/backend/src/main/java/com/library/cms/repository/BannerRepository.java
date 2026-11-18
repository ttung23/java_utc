package com.library.cms.repository;

import com.library.cms.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    Optional<Banner> findByIdAndDeletedAtIsNull(Long id);

    List<Banner> findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAsc();

    List<Banner> findByDeletedAtIsNullOrderBySortOrderAsc();
}
