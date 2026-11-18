package com.library.borrow.repository;

import com.library.borrow.entity.Renewal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RenewalRepository extends JpaRepository<Renewal, Long> {
    List<Renewal> findByItem_IdOrderByCreatedAtDesc(Long itemId);
}
