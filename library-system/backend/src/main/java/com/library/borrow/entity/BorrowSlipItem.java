package com.library.borrow.entity;

import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.CopyCondition;
import com.library.common.persistence.SoftDeletableEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "borrow_slip_items")
public class BorrowSlipItem extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slip_id", nullable = false)
    private BorrowSlip slip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowItemStatus status;

    @Column(name = "borrowed_at")
    private Instant borrowedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by")
    private User returnedBy;

    @Column(name = "renew_count", nullable = false)
    private int renewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_out", length = 20)
    private CopyCondition conditionOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_in", length = 20)
    private CopyCondition conditionIn;

    @Column(name = "return_note", columnDefinition = "TEXT")
    private String returnNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "damage_images", columnDefinition = "jsonb")
    private List<String> damageImages;
}
