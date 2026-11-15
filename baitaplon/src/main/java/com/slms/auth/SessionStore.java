package com.slms.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu trữ token phiên đăng nhập trong bộ nhớ.
 */
public final class SessionStore {

    private static final Map<String, AuthSession> SESSIONS = new ConcurrentHashMap<>();

    private SessionStore() {}

    public static void put(AuthSession session) {
        SESSIONS.put(session.getToken(), session);
    }

    public static Optional<AuthSession> get(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        AuthSession session = SESSIONS.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            SESSIONS.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public static void remove(String token) {
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    public static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
