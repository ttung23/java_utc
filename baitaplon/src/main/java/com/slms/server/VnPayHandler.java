package com.slms.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.auth.AuthSession;
import com.slms.dto.ApiResponse;
import com.slms.model.enums.UserRole;
import com.slms.payment.VnPayService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API tích hợp cổng thanh toán VNPay Sandbox cho nộp phạt thư viện.
 *
 * <ul>
 *   <li>POST /api/vnpay/create-payment — tạo URL thanh toán</li>
 *   <li>GET  /api/vnpay/return — Return URL (redirect về trang kết quả)</li>
 *   <li>GET  /api/vnpay/demo-complete — hoàn tất giao dịch chế độ local demo</li>
 *   <li>GET  /api/vnpay/ipn — IPN callback (khi server public)</li>
 * </ul>
 */
public class VnPayHandler implements HttpHandler {

    private final VnPayService vnPayService;

    public VnPayHandler(int serverPort) {
        this.vnPayService = new VnPayService(serverPort);
    }

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
            if (path.equals("/api/vnpay/create-payment") && "POST".equals(method)) {
                handleCreatePayment(exchange);
            } else if (path.equals("/api/vnpay/return") && "GET".equals(method)) {
                handleReturn(exchange);
            } else if (path.equals("/api/vnpay/demo-complete") && "GET".equals(method)) {
                handleDemoComplete(exchange);
            } else if (path.equals("/api/vnpay/ipn") && "GET".equals(method)) {
                handleIpn(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint VNPay không tồn tại"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Lỗi VNPay: " + e.getMessage()));
        }
    }

    private void handleCreatePayment(HttpExchange exchange) throws IOException {
        AuthSession session = AuthHelper.requireRoles(exchange, UserRole.READER, UserRole.LIBRARIAN, UserRole.ADMIN);
        if (session == null) {
            return;
        }

        String body = HttpHelper.readRequestBody(exchange);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        List<String> fineIds = new ArrayList<>();
        if (json.has("fineId") && !json.get("fineId").isJsonNull()) {
            fineIds.add(json.get("fineId").getAsString());
        }
        if (json.has("fineIds") && json.get("fineIds").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("fineIds");
            for (JsonElement el : arr) {
                if (!el.isJsonNull()) {
                    fineIds.add(el.getAsString());
                }
            }
        }

        String readerId;
        if (session.getRole() == UserRole.READER) {
            readerId = session.getUserId();
        } else {
            readerId = json.has("readerId") && !json.get("readerId").isJsonNull()
                    ? json.get("readerId").getAsString()
                    : null;
            if (readerId == null || readerId.isBlank()) {
                HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Thiếu readerId khi tạo thanh toán hộ độc giả"));
                return;
            }
        }

        String clientIp = resolveClientIp(exchange);
        Map<String, Object> payment = vnPayService.createPayment(readerId, fineIds, clientIp);
        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đã tạo yêu cầu thanh toán VNPay", payment));
    }

    private void handleReturn(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.getQueryParams(exchange);
        Map<String, Object> result = vnPayService.handleReturn(params);

        boolean success = Boolean.TRUE.equals(result.get("success"));
        String status = String.valueOf(result.getOrDefault("status", success ? "success" : "failed"));
        String message = String.valueOf(result.getOrDefault("message", ""));
        String txnRef = String.valueOf(result.getOrDefault("txnRef", ""));
        String amount = String.valueOf(result.getOrDefault("amount", "0"));

        String redirect = "/client/payment-result.html"
                + "?status=" + url(status)
                + "&success=" + success
                + "&txnRef=" + url(txnRef)
                + "&amount=" + url(amount)
                + "&message=" + url(message);

        HttpHelper.sendRedirect(exchange, redirect);
    }

    private void handleDemoComplete(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.getQueryParams(exchange);
        String txnRef = params.get("txnRef");
        if (txnRef == null || txnRef.isBlank()) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Thiếu txnRef"));
            return;
        }
        boolean success = !"0".equals(params.get("success")) && !"false".equalsIgnoreCase(params.getOrDefault("success", "true"));
        String redirect = vnPayService.buildLocalReturnRedirect(txnRef.trim(), success);
        HttpHelper.sendRedirect(exchange, redirect);
    }

    private void handleIpn(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.getQueryParams(exchange);
        Map<String, String> response = vnPayService.handleIpn(params);
        HttpHelper.sendJsonResponse(exchange, 200, response);
    }

    private static String resolveClientIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    private static String url(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
