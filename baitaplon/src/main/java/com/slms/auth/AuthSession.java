package com.slms.auth;

import com.slms.model.enums.UserRole;

import java.time.Instant;

/**
 * Phiên đăng nhập kèm token và vai trò.
 */
public class AuthSession {

    private final String token;
    private final UserRole role;
    private final String userId;
    private final String displayName;
    private final String avatar;
    private final Instant createdAt;
    private final Instant expiresAt;

    public AuthSession(String token, UserRole role, String userId, String displayName, Instant expiresAt) {
        this(token, role, userId, displayName, null, expiresAt);
    }

    public AuthSession(String token, UserRole role, String userId, String displayName, String avatar, Instant expiresAt) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.displayName = displayName;
        this.avatar = avatar;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getToken() {
        return token;
    }

    public UserRole getRole() {
        return role;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatar() {
        return avatar;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
