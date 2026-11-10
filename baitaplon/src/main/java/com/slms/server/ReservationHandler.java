package com.slms.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.dto.ApiResponse;
import com.slms.model.Reservation;
import com.slms.service.ReservationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Xử lý các API liên quan đến Đặt trước sách trực tuyến (Reservation).
 */
public class ReservationHandler implements HttpHandler {

    private final ReservationService reservationService = new ReservationService();

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
            if (path.equals("/api/reservations")) {
                if ("GET".equals(method)) {
                    Map<String, String> params = HttpHelper.getQueryParams(exchange);
                    String isbn = params.get("isbn");
                    List<Reservation> list;
                    if (isbn != null && !isbn.trim().isEmpty()) {
                        list = reservationService.getPendingByIsbn(isbn.trim());
                    } else {
                        list = reservationService.getAllReservations();
                    }
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(list));
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    String readerId = json.has("readerId") ? json.get("readerId").getAsString() : null;
                    String isbn = json.has("isbn") ? json.get("isbn").getAsString() : null;

                    if (readerId == null || readerId.trim().isEmpty() || isbn == null || isbn.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng cung cấp cả readerId và isbn"));
                        return;
                    }

                    Reservation reservation = reservationService.createReservation(readerId.trim(), isbn.trim());
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Đặt trước sách thành công!", reservation));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.equals("/api/reservations/cancel")) {
                if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    String reservationId = json.has("reservationId") ? json.get("reservationId").getAsString() : null;

                    if (reservationId == null || reservationId.trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Vui lòng cung cấp reservationId"));
                        return;
                    }

                    boolean success = reservationService.cancelReservation(reservationId.trim());
                    if (success) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Hủy đơn đặt trước thành công!", null));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Không thể hủy đơn đặt trước"));
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

