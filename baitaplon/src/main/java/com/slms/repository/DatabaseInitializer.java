package com.slms.repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Khởi tạo cấu trúc bảng và dữ liệu mẫu cho Database khi ứng dụng chạy lần đầu.
 */
public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (!isTableExist(conn, "BOOKS")) {
                System.out.println(">>> Chưa tìm thấy bảng BOOKS. Bắt đầu tự động khởi tạo bảng và dữ liệu mẫu...");
                executeInitScript(conn);
                System.out.println(">>> Khởi tạo cơ sở dữ liệu thành công!");
            } else {
                System.out.println(">>> Cơ sở dữ liệu đã sẵn sàng (đã tồn tại các bảng).");
            }
        } catch (Exception e) {
            System.err.println(">>> Lỗi khi kiểm tra / khởi tạo database: " + e.getMessage());
        }
    }

    private static boolean isTableExist(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) return true;
        }
        return false;
    }

    private static void executeInitScript(Connection conn) {
        // Danh sách câu lệnh DDL và DML mẫu tương thích cả Oracle lẫn H2 Oracle Mode
        String[] sqlStatements = new String[] {
            // 1. BOOKS
            "CREATE TABLE IF NOT EXISTS BOOKS (" +
            "    isbn VARCHAR2(20) NOT NULL," +
            "    title VARCHAR2(255) NOT NULL," +
            "    author VARCHAR2(150) NOT NULL," +
            "    publisher VARCHAR2(150)," +
            "    publish_year NUMBER(4)," +
            "    category VARCHAR2(100)," +
            "    CONSTRAINT pk_books PRIMARY KEY (isbn)" +
            ")",

            // 2. BOOK_COPIES
            "CREATE TABLE IF NOT EXISTS BOOK_COPIES (" +
            "    copy_id VARCHAR2(20) NOT NULL," +
            "    isbn VARCHAR2(20) NOT NULL," +
            "    barcode VARCHAR2(50) NOT NULL UNIQUE," +
            "    acquisition_date DATE DEFAULT SYSDATE," +
            "    price NUMBER(12,2) DEFAULT 0," +
            "    status VARCHAR2(20) DEFAULT 'Available'," +
            "    CONSTRAINT pk_book_copies PRIMARY KEY (copy_id)," +
            "    CONSTRAINT fk_copies_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn)" +
            ")",

            // 3. READERS
            "CREATE TABLE IF NOT EXISTS READERS (" +
            "    reader_id VARCHAR2(20) NOT NULL," +
            "    full_name VARCHAR2(100) NOT NULL," +
            "    reader_type VARCHAR2(20) DEFAULT 'STUDENT'," +
            "    expiry_date DATE NOT NULL," +
            "    balance_fine NUMBER(12,2) DEFAULT 0," +
            "    status VARCHAR2(20) DEFAULT 'ACTIVE'," +
            "    CONSTRAINT pk_readers PRIMARY KEY (reader_id)" +
            ")",

            // 4. LOANS
            "CREATE TABLE IF NOT EXISTS LOANS (" +
            "    loan_id VARCHAR2(20) NOT NULL," +
            "    reader_id VARCHAR2(20) NOT NULL," +
            "    copy_id VARCHAR2(20) NOT NULL," +
            "    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," +
            "    due_date TIMESTAMP NOT NULL," +
            "    return_date TIMESTAMP," +
            "    status VARCHAR2(20) DEFAULT 'BORROWED'," +
            "    CONSTRAINT pk_loans PRIMARY KEY (loan_id)," +
            "    CONSTRAINT fk_loans_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id)," +
            "    CONSTRAINT fk_loans_copies FOREIGN KEY (copy_id) REFERENCES BOOK_COPIES(copy_id)" +
            ")",

            // 5. FINES
            "CREATE TABLE IF NOT EXISTS FINES (" +
            "    fine_id VARCHAR2(20) NOT NULL," +
            "    loan_id VARCHAR2(20) NOT NULL," +
            "    reason VARCHAR2(255)," +
            "    fine_amount NUMBER(12,2) NOT NULL," +
            "    payment_status VARCHAR2(20) DEFAULT 'Unpaid'," +
            "    payment_date TIMESTAMP," +
            "    CONSTRAINT pk_fines PRIMARY KEY (fine_id)," +
            "    CONSTRAINT fk_fines_loans FOREIGN KEY (loan_id) REFERENCES LOANS(loan_id)" +
            ")",

            // 6. RESERVATIONS
            "CREATE TABLE IF NOT EXISTS RESERVATIONS (" +
            "    reservation_id VARCHAR2(20) NOT NULL," +
            "    reader_id VARCHAR2(20) NOT NULL," +
            "    isbn VARCHAR2(20) NOT NULL," +
            "    reservation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL," +
            "    expiry_date TIMESTAMP NOT NULL," +
            "    status VARCHAR2(20) DEFAULT 'Pending'," +
            "    CONSTRAINT pk_reservations PRIMARY KEY (reservation_id)," +
            "    CONSTRAINT fk_res_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id)," +
            "    CONSTRAINT fk_res_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn)" +
            ")",

            // Dữ liệu mẫu BOOKS
            "INSERT INTO BOOKS VALUES ('978-0-13-468599-1', 'The Pragmatic Programmer', 'David Thomas, Andrew Hunt', 'Addison-Wesley', 2019, 'Software Engineering')",
            "INSERT INTO BOOKS VALUES ('978-0-596-51774-8', 'JavaScript: The Good Parts', 'Douglas Crockford', 'O''Reilly Media', 2008, 'Programming')",
            "INSERT INTO BOOKS VALUES ('978-0-13-235088-4', 'Clean Code', 'Robert C. Martin', 'Prentice Hall', 2008, 'Software Engineering')",
            "INSERT INTO BOOKS VALUES ('978-0-201-63361-0', 'Design Patterns', 'Erich Gamma, Richard Helm', 'Addison-Wesley', 1994, 'Software Engineering')",
            "INSERT INTO BOOKS VALUES ('978-0-13-468729-2', 'Effective Java', 'Joshua Bloch', 'Addison-Wesley', 2018, 'Java')",
            "INSERT INTO BOOKS VALUES ('978-0-321-12521-7', 'Domain-Driven Design', 'Eric Evans', 'Addison-Wesley', 2003, 'Software Architecture')",
            "INSERT INTO BOOKS VALUES ('978-1-49-195016-0', 'Head First Java', 'Kathy Sierra, Bert Bates', 'O''Reilly Media', 2022, 'Java')",
            "INSERT INTO BOOKS VALUES ('978-0-13-411025-2', 'Java Concurrency in Practice', 'Brian Goetz', 'Addison-Wesley', 2006, 'Java')",
            "INSERT INTO BOOKS VALUES ('978-1-61-729008-5', 'Spring in Action', 'Craig Walls', 'Manning', 2022, 'Java')",
            "INSERT INTO BOOKS VALUES ('978-0-59-651798-4', 'Learning SQL', 'Alan Beaulieu', 'O''Reilly Media', 2020, 'Database')",

            // Dữ liệu mẫu BOOK_COPIES
            "INSERT INTO BOOK_COPIES VALUES ('CP001', '978-0-13-468599-1', 'BC-00001', CURRENT_DATE, 450000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP002', '978-0-13-468599-1', 'BC-00002', CURRENT_DATE, 450000, 'Loaned')",
            "INSERT INTO BOOK_COPIES VALUES ('CP003', '978-0-596-51774-8', 'BC-00003', CURRENT_DATE, 320000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP004', '978-0-13-235088-4', 'BC-00004', CURRENT_DATE, 380000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP005', '978-0-13-235088-4', 'BC-00005', CURRENT_DATE, 380000, 'Loaned')",
            "INSERT INTO BOOK_COPIES VALUES ('CP006', '978-0-201-63361-0', 'BC-00006', CURRENT_DATE, 520000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP007', '978-0-13-468729-2', 'BC-00007', CURRENT_DATE, 410000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP008', '978-0-13-468729-2', 'BC-00008', CURRENT_DATE, 410000, 'Loaned')",
            "INSERT INTO BOOK_COPIES VALUES ('CP009', '978-0-321-12521-7', 'BC-00009', CURRENT_DATE, 490000, 'Available')",
            "INSERT INTO BOOK_COPIES VALUES ('CP010', '978-1-49-195016-0', 'BC-00010', CURRENT_DATE, 350000, 'Available')",

            // Dữ liệu mẫu READERS
            "INSERT INTO READERS VALUES ('RD001', 'Nguyen Van An', 'STUDENT', DATE '2027-12-31', 0, 'ACTIVE')",
            "INSERT INTO READERS VALUES ('RD002', 'Tran Thi Binh', 'STUDENT', DATE '2027-12-31', 15000, 'ACTIVE')",
            "INSERT INTO READERS VALUES ('RD003', 'Le Hoang Cuong', 'STUDENT', DATE '2024-06-30', 0, 'EXPIRED')",
            "INSERT INTO READERS VALUES ('RD004', 'Pham Minh Duc', 'LECTURER', DATE '2028-12-31', 0, 'ACTIVE')",
            "INSERT INTO READERS VALUES ('RD005', 'Vo Thi Hoa', 'LECTURER', DATE '2028-06-30', 25000, 'ACTIVE')",

            // Dữ liệu mẫu LOANS
            "INSERT INTO LOANS VALUES ('LN001', 'RD001', 'CP002', TIMESTAMP '2026-11-01 09:30:00', TIMESTAMP '2026-11-15 09:30:00', NULL, 'BORROWED')",
            "INSERT INTO LOANS VALUES ('LN002', 'RD002', 'CP005', TIMESTAMP '2026-10-10 14:00:00', TIMESTAMP '2026-10-24 14:00:00', NULL, 'OVERDUE')",
            "INSERT INTO LOANS VALUES ('LN003', 'RD004', 'CP008', TIMESTAMP '2026-11-05 10:15:00', TIMESTAMP '2026-12-05 10:15:00', NULL, 'BORROWED')"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Bỏ qua lỗi nếu bảng hoặc dữ liệu đã tồn tại
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi thực thi script tạo database: " + e.getMessage());
        }
    }
}

