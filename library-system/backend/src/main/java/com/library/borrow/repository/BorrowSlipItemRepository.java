package com.library.borrow.repository;

import com.library.borrow.entity.BorrowSlipItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowSlipItemRepository extends JpaRepository<BorrowSlipItem, Long> {
}
