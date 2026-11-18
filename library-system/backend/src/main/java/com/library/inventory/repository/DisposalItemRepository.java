package com.library.inventory.repository;

import com.library.inventory.entity.DisposalItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisposalItemRepository extends JpaRepository<DisposalItem, Long> {
    List<DisposalItem> findByDisposal_Id(Long disposalId);
}
