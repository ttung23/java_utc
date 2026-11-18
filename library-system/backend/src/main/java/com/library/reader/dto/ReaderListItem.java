package com.library.reader.dto;

import com.library.common.enums.CardStatus;
import com.library.common.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReaderListItem {
    private Long id;
    private String readerCode;
    private String fullName;
    private String email;
    private String phone;
    private String readerTypeCode;
    private String readerTypeName;
    private String cardNumber;
    private CardStatus cardStatus;
    private Instant cardExpiresAt;
    private boolean blacklisted;
    private UserStatus userStatus;
}
