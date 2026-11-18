package com.library.auth.dto;

import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Instant emailVerifiedAt;
    private Instant lastLoginAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
