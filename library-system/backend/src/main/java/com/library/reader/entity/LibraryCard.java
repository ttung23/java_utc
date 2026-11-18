package com.library.reader.entity;

import com.library.common.enums.CardStatus;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "library_cards")
public class LibraryCard extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Column(name = "card_number", nullable = false, unique = true, length = 50)
    private String cardNumber;

    @Column(nullable = false, unique = true, length = 100)
    private String barcode;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "fee_paid", nullable = false, precision = 12, scale = 0)
    private BigDecimal feePaid = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_card_id")
    private LibraryCard replacedByCard;

    @Column(name = "suspended_reason", columnDefinition = "TEXT")
    private String suspendedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;
}
