package com.library.borrow.repository;

import com.library.borrow.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip, Long> {
}
