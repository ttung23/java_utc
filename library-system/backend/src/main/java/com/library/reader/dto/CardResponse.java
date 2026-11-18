package com.library.reader.dto;

import com.library.common.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class CardResponse {
    private Long id;
    private Long readerId;
    private String readerCode;
    private String readerFullName;
    private String cardNumber;
    private String barcode;
    private CardStatus status;
    private Instant issuedAt;
    private Instant expiresAt;
    private BigDecimal feePaid;
    private Long replacedByCardId;
    private String suspendedReason;
    private Instant createdAt;
    private Instant updatedAt;
}
