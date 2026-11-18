package com.slms.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.auth.AuthSession;
import com.slms.dto.ApiResponse;
import com.slms.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API xác thực: login độc giả / nhân viên, đăng ký, logout, me.
 */
public class AuthHandler implements HttpHandler {

    private final AuthService authService = new AuthService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpHelper.enableCors(exchange);
        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        try {
            if (path.equals("/api/auth/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/register") && "POST".equals(method)) {
                handleRegister(exchange);
            } else if (path.equals("/api/auth/logout") && "POST".equals(method)) {
                authService.logout(AuthHelper.extractToken(exchange));
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đăng xuất thành công", null));
            } else if (path.equals("/api/auth/me") && "GET".equals(method)) {
                Optional<AuthSession> session = AuthHelper.getSession(exchange);
                if (session.isEmpty()) {
                    HttpHelper.sendJsonResponse(exchange, 401, ApiResponse.error("Chưa đăng nhập"));
                    return;
                }
                AuthSession s = session.get();
                Map<String, Object> data = new HashMap<>();
                data.put("userId", s.getUserId());
                data.put("displayName", s.getDisplayName());
                data.put("role", s.getRole().name());
                data.put("avatar", s.getAvatar());
                data.put("expiresAt", s.getExpiresAt().toString());
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(data));
            } else if (path.equals("/api/auth/avatar") && ("PUT".equals(method) || "POST".equals(method))) {
                Optional<AuthSession> session = AuthHelper.getSession(exchange);
                if (session.isEmpty()) {
                    HttpHelper.sendJsonResponse(exchange, 401, ApiResponse.error("Chưa đăng nhập"));
                    return;
                }
                String body = HttpHelper.readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String avatarUrl = json.has("avatar") ? json.get("avatar").getAsString() : "";
                authService.updateStaffAvatar(session.get().getUserId(), avatarUrl);
                Map<String, Object> resp = new HashMap<>();
                resp.put("avatar", avatarUrl);
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Cập nhật avatar thành công", resp));
            } else if (path.equals("/api/auth/profile") && "PUT".equals(method)) {
                AuthSession session = AuthHelper.requireRoles(exchange, com.slms.model.enums.UserRole.READER);
                if (session == null) return;
                String body = HttpHelper.readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                Map<String, Object> updated = authService.updateReaderProfile(
                        session.getUserId(),
                        json.has("fullName") ? json.get("fullName").getAsString() : null,
                        json.has("email") ? json.get("email").getAsString() : null,
                        json.has("phone") ? json.get("phone").getAsString() : null
                );
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Cập nhật hồ sơ thành công", updated));
            } else if (path.equals("/api/auth/password") && "POST".equals(method)) {
                AuthSession session = AuthHelper.requireRoles(exchange, com.slms.model.enums.UserRole.READER);
                if (session == null) return;
                String body = HttpHelper.readRequestBody(exchange);
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String oldPw = json.has("oldPassword") ? json.get("oldPassword").getAsString() : null;
                String newPw = json.has("newPassword") ? json.get("newPassword").getAsString() : null;
                authService.changePassword(session.getUserId(), oldPw, newPw);
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đổi mật khẩu thành công", null));
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String mode = json.has("mode") ? json.get("mode").getAsString() : "reader";
        Map<String, Object> result;
        if ("staff".equalsIgnoreCase(mode)) {
            String username = json.has("username") ? json.get("username").getAsString() : null;
            String password = json.has("password") ? json.get("password").getAsString() : null;
            result = authService.loginStaff(username, password);
        } else {
            String readerId = json.has("readerId") ? json.get("readerId").getAsString()
                    : (json.has("username") ? json.get("username").getAsString() : null);
            String password = json.has("password") ? json.get("password").getAsString() : null;
            result = authService.loginReader(readerId, password);
        }
        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đăng nhập thành công", result));
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        Map<String, Object> result = authService.registerReader(
                json.has("fullName") ? json.get("fullName").getAsString() : null,
                json.has("readerType") ? json.get("readerType").getAsString() : "STUDENT",
                json.has("email") ? json.get("email").getAsString() : null,
                json.has("phone") ? json.get("phone").getAsString() : null,
                json.has("password") ? json.get("password").getAsString() : null
        );
        HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Đăng ký thành công", result));
    }
}
