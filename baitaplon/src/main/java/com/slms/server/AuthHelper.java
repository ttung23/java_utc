package com.slms.server;

import com.slms.auth.AuthSession;
import com.slms.auth.SessionStore;
import com.slms.dto.ApiResponse;
import com.slms.model.enums.UserRole;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Optional;

/**
 * Trích xuất và kiểm tra quyền từ header Authorization.
 */
public final class AuthHelper {

    private AuthHelper() {}

    public static Optional<AuthSession> getSession(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        String token = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
        return SessionStore.get(token);
    }

    public static String extractToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }

    /**
     * Yêu cầu đăng nhập với một trong các vai trò cho phép.
     * @return session nếu hợp lệ; null nếu đã gửi lỗi 401/403
     */
    public static AuthSession requireRoles(HttpExchange exchange, UserRole... roles) throws IOException {
        Optional<AuthSession> opt = getSession(exchange);
        if (opt.isEmpty()) {
            HttpHelper.sendJsonResponse(exchange, 401, ApiResponse.error("Vui lòng đăng nhập"));
            return null;
        }
        AuthSession session = opt.get();
        if (roles != null && roles.length > 0) {
            boolean ok = false;
            for (UserRole role : roles) {
                if (session.getRole() == role) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                HttpHelper.sendJsonResponse(exchange, 403, ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
                return null;
            }
        }
        return session;
    }

    public static boolean isStaff(AuthSession session) {
        return session != null && (session.getRole() == UserRole.LIBRARIAN || session.getRole() == UserRole.ADMIN);
    }
}
