package com.slms.server;

import com.slms.dto.ApiResponse;
import com.slms.model.Shelf;
import com.slms.service.ShelfService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Xử lý các API liên quan đến Kệ sách / Vị trí lưu kho (/api/shelves).
 */
public class ShelfHandler implements HttpHandler {

    private final ShelfService shelfService = new ShelfService();

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
            if (path.equals("/api/shelves") || path.equals("/api/shelves/")) {
                if ("GET".equals(method)) {
                    List<Shelf> shelves = shelfService.getAllShelves();
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(shelves));
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Shelf shelf = HttpHelper.getGson().fromJson(body, Shelf.class);
                    if (shelf == null || shelf.getShelfName() == null || shelf.getShelfName().trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Tên kệ sách không được để trống."));
                        return;
                    }
                    Shelf saved = shelfService.addShelf(shelf);
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Tạo kệ sách thành công", saved));
                } else if ("PUT".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Shelf shelf = HttpHelper.getGson().fromJson(body, Shelf.class);
                    if (shelfService.updateShelf(shelf)) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Cập nhật kệ sách thành công", shelf));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy kệ sách để cập nhật"));
                    }
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else if (path.startsWith("/api/shelves/")) {
                String shelfId = path.substring("/api/shelves/".length()).trim();
                if ("GET".equals(method)) {
                    java.util.Optional<Shelf> opt = shelfService.getShelfById(shelfId);
                    if (opt.isPresent()) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(opt.get()));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy kệ sách: " + shelfId));
                    }
                } else if ("PUT".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Shelf shelf = HttpHelper.getGson().fromJson(body, Shelf.class);
                    if (shelf == null) shelf = new Shelf();
                    shelf.setShelfId(shelfId);
                    if (shelfService.updateShelf(shelf)) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Cập nhật kệ sách thành công", shelf));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy kệ sách để cập nhật"));
                    }
                } else if ("DELETE".equals(method)) {
                    if (shelfService.deleteShelf(shelfId)) {
                        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Xóa kệ sách thành công", null));
                    } else {
                        HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Không tìm thấy kệ sách để xóa"));
                    }
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Đường dẫn không tồn tại"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println(">>> [ShelfHandler Error]: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Lỗi máy chủ nội bộ: " + e.getMessage()));
        }
    }
}

