package com.slms.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.dto.ApiResponse;
import com.slms.model.Fine;
import com.slms.service.FineService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Xử lý các API liên quan đến Quản lý Phạt & Thanh toán nợ phạt.
 */
public class FineHandler implements HttpHandler {

    private final FineService fineService = new FineService();

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
            if (path.equals("/api/fines")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String readerId = params.get("readerId");
                    List<Fine> fines;
                    if (readerId != null && !readerId.trim().isEmpty()) {
                        fines = fineService.getUnpaidFinesByReaderId(readerId.trim());
                    } else {
                        fines = fineService.getAllFines();
                    }
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(fines));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/fines/pay")) {
                if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    String fineId = json.has("fineId") ? json.get("fineId").getAsString() : null;
                    if (fineId == null || fineId.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng cung cấp mã phiếu phạt fineId"));
                        return;
                    }

                    boolean success = fineService.payFine(fineId.trim());
                    if (success) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Thanh toán tiền phạt thành công!", null));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Thanh toán tiền phạt thất bại"));
                    }
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }
}

