package com.slms.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slms.dto.ApiResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.*;

/**
 * REST API Handler hỗ trợ quét mã vạch bằng camera và liên kết camera điện thoại từ xa (/api/scanner/*).
 */
public class ScannerHandler implements HttpHandler {

    private static class ScannerSession {
        final String sessionId;
        final BlockingQueue<String> barcodeQueue = new LinkedBlockingQueue<>();
        volatile boolean phoneConnected = false;
        volatile boolean phoneNotified = false;
        volatile String phoneDevice = "Điện thoại";
        volatile long lastActive = System.currentTimeMillis();

        ScannerSession(String sessionId) {
            this.sessionId = sessionId;
        }

        void touch() {
            this.lastActive = System.currentTimeMillis();
        }
    }

    private static final Map<String, ScannerSession> SESSIONS = new ConcurrentHashMap<>();

    static {
        // Định kỳ dọn dẹp các session quét đã quá 30 phút không hoạt động
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scanner-session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long maxIdle = 30 * 60 * 1000L;
            SESSIONS.entrySet().removeIf(entry -> (now - entry.getValue().lastActive) > maxIdle);
        }, 5, 5, TimeUnit.MINUTES);
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
            if (path.equals("/api/scanner/session")) {
                handleCreateSession(exchange);
            } else if (path.equals("/api/scanner/push")) {
                handlePushBarcode(exchange);
            } else if (path.equals("/api/scanner/ping")) {
                handlePing(exchange);
            } else if (path.equals("/api/scanner/poll")) {
                handlePoll(exchange);
            } else if (path.equals("/api/scanner/ips")) {
                handleGetIps(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Endpoint không tồn tại"));
            }
        } catch (Exception e) {
            System.err.println(">>> [ScannerHandler Error]: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Lỗi máy chủ khi xử lý quét mã: " + e.getMessage()));
        }
    }

    private void handleCreateSession(HttpExchange exchange) throws IOException {
        String sessionId = "SCAN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ScannerSession session = new ScannerSession(sessionId);
        SESSIONS.put(sessionId, session);

        // Lấy danh sách IP mạng nội bộ (LAN) của máy chủ
        List<String> localIps = getLocalIpAddresses();
        String preferredIp = localIps.isEmpty() ? "localhost" : localIps.get(0);

        // Lấy port máy chủ từ Host header nếu có
        int port = 8080;
        String hostHeader = exchange.getRequestHeaders().getFirst("Host");
        if (hostHeader != null && hostHeader.contains(":")) {
            try {
                port = Integer.parseInt(hostHeader.split(":")[1]);
            } catch (Exception ignored) {}
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("serverPort", port);
        data.put("preferredIp", preferredIp);
        data.put("localIps", localIps);
        data.put("scannerUrl", "http://" + preferredIp + ":" + port + "/scanner.html?session=" + sessionId);

        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(data));
    }

    private void handlePing(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String sessionId = json.has("session") ? json.get("session").getAsString().trim() : "";
        String device = json.has("device") ? json.get("device").getAsString().trim() : "Điện thoại";

        ScannerSession session = SESSIONS.get(sessionId);
        if (session != null) {
            session.phoneConnected = true;
            session.phoneDevice = device;
            session.touch();
            HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đã kết nối điện thoại thành công", null));
        } else {
            HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Phiên quét không tồn tại hoặc đã hết hạn"));
        }
    }

    private void handlePushBarcode(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String sessionId = json.has("session") ? json.get("session").getAsString().trim() : "";
        String barcode = json.has("barcode") ? json.get("barcode").getAsString().trim() : "";

        if (barcode.isEmpty()) {
            HttpHelper.sendJsonResponse(exchange, 400, ApiResponse.error("Mã vạch không được để trống"));
            return;
        }

        ScannerSession session = SESSIONS.get(sessionId);
        if (session != null) {
            session.touch();
            session.phoneConnected = true;
            session.barcodeQueue.offer(barcode);

            Map<String, Object> result = new HashMap<>();
            result.put("barcode", barcode);
            result.put("sessionId", sessionId);
            result.put("timestamp", System.currentTimeMillis());

            HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok("Đã gửi mã vạch sang máy tính thành công!", result));
        } else {
            HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Phiên quét không hợp lệ hoặc đã hết hạn"));
        }
    }

    private void handlePoll(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        String sessionId = params.getOrDefault("session", "").trim();

        if (sessionId.isEmpty() || !SESSIONS.containsKey(sessionId)) {
            HttpHelper.sendJsonResponse(exchange, 404, ApiResponse.error("Phiên quét không tồn tại hoặc đã hết hạn"));
            return;
        }

        ScannerSession session = SESSIONS.get(sessionId);
        session.touch();

        try {
            // Chờ nhận barcode tối đa 15 giây (Long polling)
            String barcode = session.barcodeQueue.poll(15, TimeUnit.SECONDS);

            if (barcode != null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", "received");
                data.put("barcode", barcode);
                data.put("timestamp", System.currentTimeMillis());
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(data));
                return;
            }

            // Nếu điện thoại vừa mới kết nối lần đầu mà chưa báo
            if (session.phoneConnected && !session.phoneNotified) {
                session.phoneNotified = true;
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", "connected");
                data.put("device", session.phoneDevice);
                HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(data));
                return;
            }

            // Hết thời gian chờ mà chưa có mã mới
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", "timeout");
            data.put("phoneConnected", session.phoneConnected);
            HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(data));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Quá trình chờ bị gián đoạn"));
        }
    }

    private void handleGetIps(HttpExchange exchange) throws IOException {
        List<String> ips = getLocalIpAddresses();
        HttpHelper.sendJsonResponse(exchange, 200, ApiResponse.ok(ips));
    }

    public static List<String> getLocalIpAddresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("169.254.") && !result.contains(ip)) {
                            result.add(ip);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (result.isEmpty()) {
            result.add("localhost");
        }
        return result;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
            } else if (kv.length == 1) {
                map.put(kv[0], "");
            }
        }
        return map;
    }
}

