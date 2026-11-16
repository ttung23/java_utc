package com.slms.server;

import com.slms.auth.AuthSession;
import com.slms.dto.ApiResponse;
import com.slms.model.enums.UserRole;
import com.slms.service.ReaderPortalService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * API cổng độc giả: thống kê cá nhân, thông báo, hàng chờ đặt trước.
 */
public class ClientPortalHandler implements HttpHandler {

    private final ReaderPortalService portalService = new ReaderPortalService();

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
            AuthSession session = AuthHelper.requireRoles(exchange, UserRole.READER);
            if (session == null) {
                return;
            }
            String readerId = session.getUserId();

            if (path.equals("/api/client/stats") && "GET".equals(method)) {
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(portalService.getPersonalStats(readerId)));
            } else if (path.equals("/api/client/notifications") && "GET".equals(method)) {
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(portalService.getNotifications(readerId)));
            } else if (path.startsWith("/api/client/queue/") && "GET".equals(method)) {
                String reservationId = path.substring("/api/client/queue/".length());
                com.slms.repository.ReservationRepository repo = new com.slms.repository.ReservationRepository();
                var opt = repo.findById(reservationId);
                if (opt.isEmpty() || !readerId.equals(opt.get().getReaderId())) {
                    HttpHelper.sendJsonResponse(exchange, 403, ApiResponse.error("Không có quyền xem đơn này"));
                    return;
                }
                Map<String, Object> info = portalService.getReservationQueueInfo(reservationId);
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(info));
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        }
    }
}
