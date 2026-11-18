package com.library.reader.entity;

import com.library.common.persistence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "reader_types")
public class ReaderType extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_books", nullable = false)
    private int maxBooks;

    @Column(name = "loan_days", nullable = false)
    private int loanDays;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @Column(name = "renew_days", nullable = false)
    private int renewDays;

    @Column(name = "max_reservations", nullable = false)
    private int maxReservations;

    @Column(name = "card_fee", nullable = false, precision = 12, scale = 0)
    private BigDecimal cardFee = BigDecimal.ZERO;

    @Column(name = "card_validity_months", nullable = false)
    private int cardValidityMonths;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
