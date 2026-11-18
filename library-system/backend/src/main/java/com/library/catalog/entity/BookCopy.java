package com.library.catalog.entity;

import com.library.common.enums.AcquisitionSource;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import com.library.common.persistence.SoftDeletableEntity;
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
@Table(name = "book_copies")
public class BookCopy extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, unique = true, length = 100)
    private String barcode;

    @Column(name = "copy_no", nullable = false)
    private int copyNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopyCondition condition = CopyCondition.GOOD;

    @Column(name = "purchase_price", precision = 12, scale = 0)
    private BigDecimal purchasePrice;

    @Column(name = "acquired_at")
    private Instant acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_source", length = 20)
    private AcquisitionSource acquisitionSource;

    @Column(name = "import_item_id")
    private Long importItemId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "withdrawn_reason", columnDefinition = "TEXT")
    private String withdrawnReason;
}
