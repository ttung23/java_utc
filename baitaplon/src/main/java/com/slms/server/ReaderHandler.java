package com.slms.server;

import com.slms.dto.ApiResponse;
import com.slms.model.Reader;
import com.slms.repository.LoanRepository;
import com.slms.repository.ReaderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xử lý các API liên quan đến Độc giả và kiểm tra tính hợp lệ khi mượn sách.
 */
public class ReaderHandler implements HttpHandler {

    private final ReaderRepository readerRepository = new ReaderRepository();
    private final LoanRepository loanRepository = new LoanRepository();

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
            if (path.equals("/api/readers/eligibility")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String readerId = params.get("readerId");

                    if (readerId == null || readerId.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng cung cấp mã độc giả"));
                        return;
                    }

                    Optional<Reader> readerOpt = readerRepository.findById(readerId.trim());
                    if (readerOpt.isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy độc giả với mã: " + readerId));
                        return;
                    }

                    Reader reader = readerOpt.get();
                    int activeLoansCount = loanRepository.countActiveLoansByReaderId(reader.getReaderId());
                    boolean eligible = reader.canBorrow(activeLoansCount);

                    Map<String, Object> result = new HashMap<>();
                    result.put("readerId", reader.getReaderId());
                    result.put("fullName", reader.getFullName());
                    result.put("readerType", reader.getType().name());
                    result.put("status", reader.getStatus());
                    result.put("expiryDate", reader.getExpiryDate().toString());
                    result.put("balanceFine", reader.getBalanceFine());
                    result.put("currentBorrowedCount", activeLoansCount);
                    result.put("maxBooks", reader.getType().getMaxBooks());
                    result.put("maxBorrowDays", reader.getType().getMaxBorrowDays());
                    result.put("eligible", eligible);

                    if (!eligible) {
                        if (!"ACTIVE".equalsIgnoreCase(reader.getStatus())) {
                            result.put("reason", "Thẻ độc giả đang ở trạng thái: " + reader.getStatus());
                        } else if (reader.getExpiryDate().isBefore(LocalDate.now())) {
                            result.put("reason", "Thẻ độc giả đã hết hạn sử dụng vào ngày " + reader.getExpiryDate());
                        } else if (reader.getBalanceFine() > 50000.0) {
                            result.put("reason", "Độc giả đang nợ tiền phạt (" + String.format("%,.0f", reader.getBalanceFine()) + " VNĐ), vượt quá hạn mức cho phép 50.000 VNĐ");
                        } else if (activeLoansCount >= reader.getType().getMaxBooks()) {
                            result.put("reason", "Đã đạt số lượng mượn tối đa (" + activeLoansCount + "/" + reader.getType().getMaxBooks() + " cuốn)");
                        }
                    } else {
                        result.put("reason", "Độc giả đủ điều kiện mượn sách (còn mượn được " + (reader.getType().getMaxBooks() - activeLoansCount) + " cuốn)");
                    }

                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(result));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/readers")) {
                if ("GET".equals(method)) {
                    List<Reader> readers = readerRepository.findAll();
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(readers));
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Reader reader = HttpHelper.getGson().fromJson(body, Reader.class);
                    if (reader.getStatus() == null) {
                        reader.setStatus("ACTIVE");
                    }
                    if (reader.getBalanceFine() < 0) {
                        reader.setBalanceFine(0);
                    }
                    Reader saved = readerRepository.save(reader);
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Thêm độc giả thành công", saved));
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

