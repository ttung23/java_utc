package com.library.fine.entity;

import com.library.borrow.entity.BorrowSlipItem;
import com.library.common.enums.FineStatus;
import com.library.common.enums.FineType;
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

@Getter
@Setter
@Entity
@Table(name = "fines")
public class Fine extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slip_item_id")
    private BorrowSlipItem slipItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FineType type;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status;

    @Column(name = "overdue_days")
    private Integer overdueDays;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by")
    private User waivedBy;

    @Column(name = "waived_reason", columnDefinition = "TEXT")
    private String waivedReason;
}
