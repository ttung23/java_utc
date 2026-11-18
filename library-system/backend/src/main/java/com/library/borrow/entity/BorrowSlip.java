package com.library.borrow.entity;

import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.persistence.SoftDeletableEntity;
import com.library.reader.entity.Reader;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "borrow_slips")
public class BorrowSlip extends SoftDeletableEntity {

    @Column(name = "slip_code", nullable = false, unique = true, length = 50)
    private String slipCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BorrowSlipStatus status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "pickup_deadline")
    private Instant pickupDeadline;

    @Column(name = "borrowed_at")
    private Instant borrowedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
}
