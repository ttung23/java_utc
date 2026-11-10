package com.slms.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.dto.ApiResponse;
import com.slms.dto.LoanDTO;
import com.slms.dto.ReturnResultDTO;
import com.slms.model.Loan;
import com.slms.service.LoanService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Xử lý các API cho Ca sử dụng Mượn sách (UC21) và Trả sách / Xử lý sự cố (UC22).
 */
public class LoanHandler implements HttpHandler {

    private final LoanService loanService = new LoanService();

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
            if (path.equals("/api/loans/borrow")) {
                // UC21: LẬP PHIẾU MƯỢN SÁCH
                if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    String readerId = json.has("readerId") ? json.get("readerId").getAsString() : null;
                    String barcode = json.has("barcode") ? json.get("barcode").getAsString() : null;

                    if (readerId == null || readerId.trim().isEmpty() || barcode == null || barcode.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng cung cấp cả readerId và barcode"));
                        return;
                    }

                    LoanDTO loanDto = loanService.createLoan(readerId.trim(), barcode.trim());
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Lập phiếu mượn sách thành công!", loanDto));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/loans/return")) {
                // UC22: TIẾP NHẬN TRẢ SÁCH & XỬ LÝ SỰ CỐ
                if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    String barcode = json.has("barcode") ? json.get("barcode").getAsString() : null;
                    boolean isDamaged = json.has("isDamaged") && json.get("isDamaged").getAsBoolean();
                    boolean isLost = json.has("isLost") && json.get("isLost").getAsBoolean();
                    double damagePercent = json.has("damagePercent") ? json.get("damagePercent").getAsDouble() : 100.0;

                    if (barcode == null || barcode.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng quét hoặc nhập mã vạch barcode sách"));
                        return;
                    }

                    ReturnResultDTO result = loanService.processReturn(barcode.trim(), isDamaged, isLost, damagePercent);
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Xử lý trả sách thành công!", result));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/loans")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String status = params.get("status");
                    List<Loan> loans;
                    if ("OVERDUE".equalsIgnoreCase(status)) {
                        loans = loanService.getOverdueLoans();
                    } else {
                        loans = loanService.getAllLoans();
                    }
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(loans));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        }
    }
}

