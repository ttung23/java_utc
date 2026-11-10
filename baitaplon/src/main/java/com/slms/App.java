package com.slms;

import com.slms.repository.DatabaseConnection;
import com.slms.server.BookHandler;
import com.slms.server.FineHandler;
import com.slms.server.LoanHandler;
import com.slms.server.ReaderHandler;
import com.slms.server.ReservationHandler;
import com.slms.server.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Lớp khởi động chính cho Hệ Thống Quản Lý Thư Viện Thông Minh (SLMS).
 */
public class App {

    private static final int PORT = 8080;
    private static final long START_TIME = System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("       SMART LIBRARY MANAGEMENT SYSTEM (SLMS) - JAVA & DATABASE        ");
        System.out.println("========================================================================");

        // 1. Kiểm tra và khởi tạo cơ sở dữ liệu
        try {
            System.out.println(">>> Đang kiểm tra kết nối cơ sở dữ liệu...");
            DatabaseConnection.getConnection();
            System.out.println(">>> Loại Database hiện tại: " + DatabaseConnection.getDatabaseType());
        } catch (Exception e) {
            System.err.println(">>> Cảnh báo khi kết nối Database: " + e.getMessage());
        }

        // 2. Khởi chạy HTTP Web Server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Đăng ký các REST API Handlers
            server.createContext("/api/books", new BookHandler());
            server.createContext("/api/readers", new ReaderHandler());
            server.createContext("/api/loans", new LoanHandler());
            server.createContext("/api/fines", new FineHandler());
            server.createContext("/api/reservations", new ReservationHandler());

            // Endpoint kiểm tra phiên bản phục vụ Auto Live Reload
            server.createContext("/api/version", exchange -> {
                com.slms.server.HttpHelper.enableCors(exchange);
                byte[] resp = String.valueOf(START_TIME).getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, resp.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            });

            // Đăng ký Static File Handler cho Frontend Web
            server.createContext("/", new StaticFileHandler());

            // Thiết lập Thread Pool đa luồng
            server.setExecutor(Executors.newFixedThreadPool(12));

            // Bắt đầu lắng nghe
            server.start();

            System.out.println("------------------------------------------------------------------------");
            System.out.println(">>> SERVER ĐANG CHẠY TẠI: http://localhost:" + PORT);
            System.out.println(">>> Hãy mở trình duyệt và truy cập: http://localhost:" + PORT);
            System.out.println("------------------------------------------------------------------------");
            System.out.println(">>> Nhấn Ctrl+C trong terminal để dừng server.");
            System.out.println("========================================================================");

        } catch (IOException e) {
            System.err.println(">>> Lỗi khi khởi động HTTP Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

