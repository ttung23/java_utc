package com.slms.server;

import com.slms.dto.ApiResponse;
import com.slms.model.Category;
import com.slms.service.CategoryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Xử lý các API liên quan đến Thể loại sách (/api/categories).
 */
public class CategoryHandler implements HttpHandler {

    private final CategoryService categoryService = new CategoryService();

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
            if (path.equals("/api/categories") || path.equals("/api/categories/")) {
                if ("GET".equals(method)) {
                    List<Category> categories = categoryService.getAllCategories();
                    HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(categories));
                } else if ("POST".equals(method)) {
                    String body = HttpHelper.readRequestBody(exchange);
                    Category category = HttpHelper.getGson().fromJson(body, Category.class);
                    if (category == null || category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
                        HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Tên thể loại không được để trống."));
                        return;
                    }
                    Category saved = categoryService.addCategory(category);
                    HttpHelper.sendJsonResponse(exchange, 201, ApiResponse.ok("Tạo thể loại thành công", saved));
                } else {
                    HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Đường dẫn không tồn tại"));
            }
        } catch (IllegalArgumentException e) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println(">>> [CategoryHandler Error]: " + e.getMessage());
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Lỗi máy chủ nội bộ: " + e.getMessage()));
        }
    }
}

