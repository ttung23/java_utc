package com.slms.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối cơ sở dữ liệu.
 * Kết nối trực tiếp đến Oracle Database 23ai Free trên máy local (Port 1521, SID/Service: FREE).
 * Có cơ chế tự động fallback nếu service Oracle chưa được bật.
 */
public class DatabaseConnection {

    // ==================== Cấu hình Oracle Database 23ai Thật ====================
    /** URL kết nối Oracle 23ai: jdbc:oracle:thin:@localhost:1521:FREE */
    private static final String ORACLE_URL = System.getProperty("oracle.url", "jdbc:oracle:thin:@localhost:1521:FREE");
    private static final String ORACLE_USER = System.getProperty("oracle.user", "slms_user");
    private static final String ORACLE_PASSWORD = System.getProperty("oracle.password", "slms_password");

    // Cấu hình dự phòng Local DB
    private static final String FALLBACK_URL = "jdbc:h2:./database/slms;MODE=Oracle;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    private static final String FALLBACK_USER = "sa";
    private static final String FALLBACK_PASSWORD = "";

    private static Boolean useOracle = null;
    private static boolean initialized = false;

    private DatabaseConnection() {}

    /**
     * Lấy kết nối cơ sở dữ liệu.
     * Ưu tiên kết nối Oracle Database 23ai thật (slms_user / slms_password @ localhost:1521:FREE).
     */
    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = null;

        // Kiểm tra kết nối Oracle 23ai thật trước
        if (useOracle == null) {
            try {
                Class.forName("oracle.jdbc.OracleDriver");
                conn = DriverManager.getConnection(ORACLE_URL, ORACLE_USER, ORACLE_PASSWORD);
                useOracle = true;
                System.out.println(">>> ========================================================");
                System.out.println(">>> [DB] ĐÃ KẾT NỐI THÀNH CÔNG ĐẾN ORACLE DATABASE 23ai THẬT!");
                System.out.println(">>> URL: " + ORACLE_URL);
                System.out.println(">>> User: " + ORACLE_USER);
                System.out.println(">>> ========================================================");
            } catch (Exception e) {
                System.out.println(">>> [DB] Chưa kết nối được Oracle thật (" + e.getMessage() + ").");
                System.out.println(">>> [DB] Tự động chuyển sang chế độ Local Embedded DB (Tương thích cú pháp Oracle).");
                useOracle = false;
            }
        }

        if (conn == null) {
            if (Boolean.TRUE.equals(useOracle)) {
                try {
                    conn = DriverManager.getConnection(ORACLE_URL, ORACLE_USER, ORACLE_PASSWORD);
                } catch (SQLException e) {
                    System.err.println(">>> [DB] Mất kết nối Oracle, chuyển sang Embedded Database.");
                    useOracle = false;
                }
            }

            if (Boolean.FALSE.equals(useOracle)) {
                try {
                    Class.forName("org.h2.Driver");
                    conn = DriverManager.getConnection(FALLBACK_URL, FALLBACK_USER, FALLBACK_PASSWORD);
                } catch (ClassNotFoundException e) {
                    throw new SQLException("Không tìm thấy Driver cơ sở dữ liệu trong classpath.", e);
                }
            }
        }

        // Tự động kiểm tra bảng nếu chạy chế độ dự phòng
        if (!initialized && conn != null) {
            initialized = true;
            if (Boolean.FALSE.equals(useOracle)) {
                DatabaseInitializer.initialize();
            }
        }

        return conn;
    }

    public static String getDatabaseType() {
        return Boolean.TRUE.equals(useOracle) ? "Oracle Database 23ai (slms_user@FREE)" : "Local Embedded DB (Oracle Mode)";
    }
}
