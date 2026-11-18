package com.library.inventory.entity;

import com.library.common.enums.InventoryAuditStatus;
import com.library.common.persistence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory_audits")
public class InventoryAudit extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "scope_type", length = 50)
    private String scopeType;

    @Column(name = "scope_value")
    private String scopeValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryAuditStatus status = InventoryAuditStatus.OPEN;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
