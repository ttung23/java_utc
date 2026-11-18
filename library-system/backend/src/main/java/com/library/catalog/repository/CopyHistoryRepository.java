package com.library.catalog.repository;

import com.library.catalog.entity.CopyHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CopyHistoryRepository extends JpaRepository<CopyHistory, Long> {
    List<CopyHistory> findByCopy_IdOrderByCreatedAtDesc(Long copyId);
}
