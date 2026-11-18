package com.library.inventory.entity;

import com.library.catalog.entity.BookCopy;
import com.library.catalog.entity.Location;
import com.library.common.enums.InventoryAuditResult;
import com.library.common.persistence.BaseEntity;
import com.library.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory_audit_items")
public class InventoryAuditItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private InventoryAudit audit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expected_location_id")
    private Location expectedLocation;

    private Boolean found;

    @Column(name = "found_at")
    private Instant foundAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private InventoryAuditResult result;

    @Column(columnDefinition = "TEXT")
    private String note;
}
