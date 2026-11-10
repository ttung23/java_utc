package com.slms.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Phục vụ các tài nguyên giao diện tĩnh (HTML, CSS, JavaScript) cho trình duyệt.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path webappDir;

    public StaticFileHandler() {
        // Tìm thư mục webapp
        Path candidate = Paths.get("src", "main", "webapp");
        if (!Files.exists(candidate)) {
            candidate = Paths.get("baitaplon", "src", "main", "webapp");
        }
        if (!Files.exists(candidate)) {
            candidate = Paths.get("webapp");
        }
        this.webappDir = candidate.toAbsolutePath();
        System.out.println(">>> [Web] Static web root: " + this.webappDir);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Chuẩn hóa đường dẫn: nếu truy cập "/" thì chuyển về "index.html"
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Loại bỏ dấu slash ở đầu
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        Path targetFile = webappDir.resolve(relativePath).normalize();

        // Ngăn chặn Directory Traversal
        if (!targetFile.startsWith(webappDir)) {
            send404(exchange, "Truy cập bị từ chối");
            return;
        }

        if (Files.exists(targetFile) && !Files.isDirectory(targetFile)) {
            String mimeType = getMimeType(targetFile.toString());
            exchange.getResponseHeaders().set("Content-Type", mimeType + "; charset=UTF-8");
            HttpHelper.enableCors(exchange);

            byte[] fileBytes = Files.readAllBytes(targetFile);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        } else {
            send404(exchange, "Không tìm thấy file: " + path);
        }
    }

    private void send404(HttpExchange exchange, String message) throws IOException {
        byte[] bytes = ("<h1>404 Not Found</h1><p>" + message + "</p>").getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "text/plain";
    }
}

