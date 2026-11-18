package com.library.reader.dto;

import com.library.common.enums.CardStatus;
import com.library.common.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class ReaderDetailResponse {
    private Long id;
    private Long userId;
    private String readerCode;
    private String fullName;
    private String email;
    private String phone;
    private UserStatus userStatus;
    private Long readerTypeId;
    private String readerTypeCode;
    private String readerTypeName;
    private LocalDate dob;
    private String gender;
    private String address;
    private String idNumber;
    private String studentCode;
    private String organization;
    private String emergencyContact;
    private String note;
    private boolean blacklisted;
    private String blacklistReason;
    private int noShowCount;
    private Instant registeredAt;
    private Long cardId;
    private String cardNumber;
    private String cardBarcode;
    private CardStatus cardStatus;
    private Instant cardIssuedAt;
    private Instant cardExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** B2 core: chưa triển khai — trả list rỗng */
    @Builder.Default
    private List<Object> borrowing = Collections.emptyList();
    @Builder.Default
    private List<Object> history = Collections.emptyList();
    @Builder.Default
    private List<Object> fines = Collections.emptyList();
    @Builder.Default
    private List<Object> reservations = Collections.emptyList();
}
