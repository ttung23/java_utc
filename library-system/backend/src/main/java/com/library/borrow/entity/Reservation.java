package com.library.borrow.entity;

import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.common.enums.ReservationStatus;
import com.library.common.persistence.SoftDeletableEntity;
import com.library.reader.entity.Reader;
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
@Table(name = "reservations")
public class Reservation extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_copy_id")
    private BookCopy assignedCopy;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "ready_until")
    private Instant readyUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_slip_id")
    private BorrowSlip fulfilledSlip;
}
