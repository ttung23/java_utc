package com.slms.repository;

import com.slms.model.StaffUser;
import com.slms.model.enums.UserRole;
import com.slms.service.AuthService;
import com.slms.util.PasswordUtil;

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
            upgradeSchema(conn);
            ensureAuthDefaults();
            seedDemoFinesIfNeeded(conn);
        } catch (Exception e) {
            System.err.println(">>> Lỗi khi kiểm tra / khởi tạo database: " + e.getMessage());
        }
    }

    /**
     * Tạo / bổ sung hóa đơn phạt Unpaid để demo VNPay.
     * An toàn khi restart nhiều lần (bỏ qua nếu mã đã tồn tại).
     */
    private static void seedDemoFinesIfNeeded(Connection conn) {
        ensureDemoFine(conn, "FNDEMO01", "LN001", "Phat tre han demo 2 ngay", 10000);
        ensureDemoFine(conn, "FNDEMO02", "LN001", "Phat hu hong bia sach demo", 25000);
        ensureDemoFine(conn, "FNDEMO03", "LN002", "Phat tre han demo RD002", 20000);
        // Bộ test mới cho RD001 (dùng khi các khoản cũ đã Paid)
        ensureDemoFine(conn, "FNTEST10", "LN001", "Phat tre han test 3 ngay", 15000);
        ensureDemoFine(conn, "FNTEST11", "LN001", "Phat mat tem barcode demo", 10000);
        ensureDemoFine(conn, "FNTEST12", "LN001", "Phat tra sach tre han them", 20000);
        ensureDemoFine(conn, "FNTEST20", "LN002", "Phat tre han test RD002", 30000);
        refreshReaderFineBalances(conn);
        System.out.println(">>> [DB] Đã đảm bảo dữ liệu phạt demo/test (FNDEMO*, FNTEST*).");
    }

    /** Public để API seed-test gọi lại khi đang chạy server. */
    public static void seedTestFinesNow() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            seedDemoFinesIfNeeded(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Không seed được dữ liệu phạt test: " + e.getMessage(), e);
        }
    }

    private static void ensureDemoFine(Connection conn, String fineId, String loanId,
                                       String reason, double amount) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM FINES WHERE fine_id = '" + fineId + "'")) {
            if (rs.next() && rs.getInt("cnt") > 0) {
                return;
            }
        } catch (SQLException e) {
            System.err.println(">>> [DB] Không kiểm tra fine " + fineId + ": " + e.getMessage());
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "INSERT INTO FINES (fine_id, loan_id, reason, fine_amount, payment_status, payment_date) "
                            + "VALUES ('" + fineId + "', '" + loanId + "', '" + reason + "', "
                            + amount + ", 'Unpaid', NULL)");
        } catch (SQLException e) {
            System.err.println(">>> [DB] Seed fine " + fineId + " bỏ qua: " + e.getMessage());
        }
    }

    private static void refreshReaderFineBalances(Connection conn) {
        String sql = "UPDATE READERS r SET balance_fine = ("
                + " SELECT COALESCE(SUM(f.fine_amount), 0) FROM FINES f "
                + " JOIN LOANS l ON f.loan_id = l.loan_id "
                + " WHERE l.reader_id = r.reader_id AND f.payment_status = 'Unpaid'"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println(">>> [DB] Không cập nhật balance_fine: " + e.getMessage());
        }
    }

    /**
     * Nâng cấp schema hiện có: thêm cột auth/profile + bảng STAFF_USERS.
     * Tương thích hoàn toàn cú pháp Oracle Database 23ai và H2 Database.
     */
    public static void upgradeSchema(Connection conn) {
        addColumnIfMissing(conn, "READERS", "EMAIL", "VARCHAR2(150)");
        addColumnIfMissing(conn, "READERS", "PHONE", "VARCHAR2(30)");
        addColumnIfMissing(conn, "READERS", "PASSWORD_HASH", "VARCHAR2(100)");

        createStaffTableIfMissing(conn);
        addColumnIfMissing(conn, "STAFF_USERS", "AVATAR", "VARCHAR2(500)");
        createCategoriesTableIfMissing(conn);
        addColumnIfMissing(conn, "BOOKS", "CATEGORY_ID", "VARCHAR2(50)");
        migrateBookCategories(conn);
        addColumnIfMissing(conn, "LOANS", "RENEWAL_COUNT", "NUMBER DEFAULT 0");
        addColumnIfMissing(conn, "BOOKS", "LOCATION", "VARCHAR2(100)");
        seedBookLocations(conn);
        createShelvesTableIfMissing(conn);
        addColumnIfMissing(conn, "BOOKS", "SHELF_ID", "VARCHAR2(50)");
        migrateBookShelves(conn);
        System.out.println(">>> [DB] Schema upgrade (auth/profile/categories/loans/locations/shelves) đã hoàn tất.");
    }

    private static void seedBookLocations(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ B1 - Tầng 2 (CNTT)' WHERE isbn = '978-0-13-468599-1' AND location IS NULL");
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ B2 - Tầng 2 (Phần mềm)' WHERE isbn = '978-0-13-235088-4' AND location IS NULL");
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ B2 - Tầng 2 (Kiến trúc)' WHERE isbn = '978-0-201-63361-0' AND location IS NULL");
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ B3 - Tầng 2 (Web Dev)' WHERE isbn = '978-0-596-51774-8' AND location IS NULL");
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ A1 - Tầng 1 (Cơ sở dữ liệu)' WHERE isbn = '978-0-13-468729-2' AND location IS NULL");
            stmt.executeUpdate("UPDATE BOOKS SET location = 'Kệ A2 - Tầng 1 (Khu Đại cương)' WHERE location IS NULL");
        } catch (SQLException ignored) {}
    }

    private static void addColumnIfMissing(Connection conn, String tableName, String columnName, String columnType) {
        if (!isColumnExist(conn, tableName, columnName)) {
            // Cú pháp chuẩn Oracle: ALTER TABLE table_name ADD (column_name type)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + tableName + " ADD (" + columnName + " " + columnType + ")");
                System.out.println(">>> [DB] Đã thêm cột " + columnName + " vào bảng " + tableName);
            } catch (SQLException e) {
                // Thử cú pháp không ngoặc đơn nếu có hệ QT CSDL khác: ALTER TABLE table_name ADD column_name type
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType);
                    System.out.println(">>> [DB] Đã thêm cột " + columnName + " vào bảng " + tableName);
                } catch (SQLException ex) {
                    // ORA-01430: column being added already exists in table
                    if (ex.getErrorCode() != 1430 && !ex.getMessage().toLowerCase().contains("already exists")) {
                        System.err.println(">>> [DB] Cảnh báo khi thêm cột " + columnName + ": " + ex.getMessage());
                    }
                }
            }
        }
    }

    private static void createStaffTableIfMissing(Connection conn) {
        if (!isTableExist(conn, "STAFF_USERS")) {
            String sql = "CREATE TABLE STAFF_USERS (" +
                    " staff_id VARCHAR2(20) NOT NULL," +
                    " username VARCHAR2(50) NOT NULL UNIQUE," +
                    " full_name VARCHAR2(100) NOT NULL," +
                    " password_hash VARCHAR2(100) NOT NULL," +
                    " role VARCHAR2(20) NOT NULL," +
                    " status VARCHAR2(20) DEFAULT 'ACTIVE'," +
                    " avatar VARCHAR2(500)," +
                    " CONSTRAINT pk_staff_users PRIMARY KEY (staff_id)" +
                    ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println(">>> [DB] Đã tạo bảng STAFF_USERS thành công.");
            } catch (SQLException e) {
                // ORA-00955: name is already used by an existing object
                if (e.getErrorCode() != 955 && !e.getMessage().toLowerCase().contains("already exists")) {
                    System.err.println(">>> [DB] Cảnh báo khi tạo bảng STAFF_USERS: " + e.getMessage());
                }
            }
        }
    }

    private static void createCategoriesTableIfMissing(Connection conn) {
        if (!isTableExist(conn, "CATEGORIES")) {
            String sql = "CREATE TABLE CATEGORIES (" +
                    "    category_id VARCHAR2(50) NOT NULL," +
                    "    category_name VARCHAR2(100) NOT NULL UNIQUE," +
                    "    description VARCHAR2(255)," +
                    "    CONSTRAINT pk_categories PRIMARY KEY (category_id)" +
                    ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println(">>> [DB] Đã tạo bảng CATEGORIES thành công.");
            } catch (SQLException e) {
                if (e.getErrorCode() != 955 && !e.getMessage().toLowerCase().contains("already exists")) {
                    System.err.println(">>> [DB] Cảnh báo khi tạo bảng CATEGORIES: " + e.getMessage());
                }
            }
        }
    }

    private static void migrateBookCategories(Connection conn) {
        try {
            // Lấy danh sách category từ bảng BOOKS
            java.util.Set<String> distinctCategories = new java.util.LinkedHashSet<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM BOOKS WHERE category IS NOT NULL")) {
                while (rs.next()) {
                    String cat = rs.getString(1);
                    if (cat != null && !cat.trim().isEmpty()) {
                        distinctCategories.add(cat.trim());
                    }
                }
            }

            // Nếu chưa có thể loại nào thì thêm một số thể loại cơ bản
            if (distinctCategories.isEmpty()) {
                distinctCategories.add("Software Engineering");
                distinctCategories.add("Programming");
                distinctCategories.add("Database");
                distinctCategories.add("General");
                distinctCategories.add("Khoa học máy tính");
            }

            CategoryRepository catRepo = new CategoryRepository();
            for (String catName : distinctCategories) {
                catRepo.getOrCreateByName(catName);
            }

            // Cập nhật category_id trong BOOKS cho những cuốn chưa có
            java.util.List<com.slms.model.Category> allCats = catRepo.findAll();
            for (com.slms.model.Category c : allCats) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE BOOKS SET category_id = ? WHERE (category_id IS NULL OR category_id = '') AND LOWER(TRIM(category)) = LOWER(TRIM(?))")) {
                    ps.setString(1, c.getCategoryId());
                    ps.setString(2, c.getCategoryName());
                    ps.executeUpdate();
                }
            }

            // Thử thêm ràng buộc khóa ngoại (nếu chưa có)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE BOOKS ADD CONSTRAINT fk_books_categories FOREIGN KEY (category_id) REFERENCES CATEGORIES(category_id)");
                System.out.println(">>> [DB] Đã thêm ràng buộc khóa ngoại fk_books_categories.");
            } catch (SQLException ignored) {
                // Ràng buộc đã tồn tại hoặc không hỗ trợ
            }
        } catch (Exception e) {
            System.err.println(">>> [DB] Cảnh báo khi migrate categories: " + e.getMessage());
        }
    }

    private static void createShelvesTableIfMissing(Connection conn) {
        if (!isTableExist(conn, "SHELVES")) {
            String sql = "CREATE TABLE SHELVES (" +
                    "    shelf_id VARCHAR2(50) NOT NULL," +
                    "    shelf_name VARCHAR2(100) NOT NULL UNIQUE," +
                    "    description VARCHAR2(500)," +
                    "    CONSTRAINT pk_shelves PRIMARY KEY (shelf_id)" +
                    ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println(">>> [DB] Đã tạo bảng SHELVES thành công.");
            } catch (SQLException e) {
                if (e.getErrorCode() != 955 && !e.getMessage().toLowerCase().contains("already exists")) {
                    System.err.println(">>> [DB] Cảnh báo khi tạo bảng SHELVES: " + e.getMessage());
                }
            }
        }
    }

    private static void migrateBookShelves(Connection conn) {
        try {
            java.util.Set<String> distinctLocations = new java.util.LinkedHashSet<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT DISTINCT location FROM BOOKS WHERE location IS NOT NULL")) {
                while (rs.next()) {
                    String loc = rs.getString(1);
                    if (loc != null && !loc.trim().isEmpty()) {
                        distinctLocations.add(loc.trim());
                    }
                }
            }

            if (distinctLocations.isEmpty()) {
                distinctLocations.add("Kệ A1 - Tầng 1 (Cơ sở dữ liệu)");
                distinctLocations.add("Kệ A2 - Tầng 1 (Khu Đại cương)");
                distinctLocations.add("Kệ B1 - Tầng 2 (CNTT)");
                distinctLocations.add("Kệ B2 - Tầng 2 (Phần mềm & Kiến trúc)");
                distinctLocations.add("Kệ B3 - Tầng 2 (Web Dev)");
                distinctLocations.add("Kệ C1 - Tầng 3 (Sách Ngoại văn)");
                distinctLocations.add("Kệ C2 - Tầng 3 (Luận văn & Nghiên cứu)");
                distinctLocations.add("Quầy lưu trữ đặc biệt (Tầng 1)");
            }

            ShelfRepository shelfRepo = new ShelfRepository();
            for (String shelfName : distinctLocations) {
                shelfRepo.getOrCreateByName(shelfName);
            }

            // Cập nhật shelf_id trong BOOKS cho những cuốn chưa có
            java.util.List<com.slms.model.Shelf> allShelves = shelfRepo.findAll();
            for (com.slms.model.Shelf s : allShelves) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE BOOKS SET shelf_id = ? WHERE (shelf_id IS NULL OR shelf_id = '') AND LOWER(TRIM(location)) = LOWER(TRIM(?))")) {
                    ps.setString(1, s.getShelfId());
                    ps.setString(2, s.getShelfName());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println(">>> [DB] Cảnh báo khi migrate shelves: " + e.getMessage());
        }
    }

    private static boolean isColumnExist(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase())) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    /**
     * Gán mật khẩu mặc định cho độc giả cũ + tạo tài khoản thủ thư/admin demo.
     */
    public static void ensureAuthDefaults() {
        try {
            ReaderRepository readerRepository = new ReaderRepository();
            String defaultHash = PasswordUtil.hash(AuthService.DEFAULT_READER_PASSWORD);
            for (String id : readerRepository.findIdsMissingPassword()) {
                readerRepository.updatePassword(id, defaultHash);
            }

            StaffRepository staffRepository = new StaffRepository();
            if (staffRepository.count() == 0) {
                staffRepository.save(new StaffUser(
                        "ST001", "librarian", "Thu Thu Demo",
                        PasswordUtil.hash("123456"), UserRole.LIBRARIAN, "ACTIVE", "images/avatar-staff.svg"));
                staffRepository.save(new StaffUser(
                        "ST002", "admin", "Admin SLMS",
                        PasswordUtil.hash("admin123"), UserRole.ADMIN, "ACTIVE", "images/avatar-admin.svg"));
                System.out.println(">>> [Auth] Đã tạo tài khoản staff demo: librarian/123456, admin/admin123");
            } else {
                // Cập nhật avatar mặc định cho các tài khoản hiện có nếu chưa có avatar
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE STAFF_USERS SET avatar = 'images/avatar-admin.svg' WHERE role = 'ADMIN' AND (avatar IS NULL OR avatar = '')");
                    stmt.executeUpdate("UPDATE STAFF_USERS SET avatar = 'images/avatar-staff.svg' WHERE role != 'ADMIN' AND (avatar IS NULL OR avatar = '')");
                } catch (Exception ignored) {
                }
            }
            System.out.println(">>> [Auth] Mật khẩu mặc định độc giả demo: " + AuthService.DEFAULT_READER_PASSWORD);
        } catch (Exception e) {
            System.err.println(">>> [Auth] Lỗi khởi tạo mặc định: " + e.getMessage());
        }
    }

    private static boolean isTableExist(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                if (rs.next()) return true;
            }
        } catch (SQLException ignored) {
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
            "    email VARCHAR2(150)," +
            "    phone VARCHAR2(30)," +
            "    password_hash VARCHAR2(100)," +
            "    CONSTRAINT pk_readers PRIMARY KEY (reader_id)" +
            ")",

            // 3b. STAFF_USERS
            "CREATE TABLE IF NOT EXISTS STAFF_USERS (" +
            "    staff_id VARCHAR2(20) NOT NULL," +
            "    username VARCHAR2(50) NOT NULL UNIQUE," +
            "    full_name VARCHAR2(100) NOT NULL," +
            "    password_hash VARCHAR2(100) NOT NULL," +
            "    role VARCHAR2(20) NOT NULL," +
            "    status VARCHAR2(20) DEFAULT 'ACTIVE'," +
            "    CONSTRAINT pk_staff_users PRIMARY KEY (staff_id)" +
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

            // Dữ liệu mẫu READERS (password_hash gán sau bởi ensureAuthDefaults)
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES ('RD001', 'Nguyen Van An', 'STUDENT', DATE '2027-12-31', 0, 'ACTIVE', 'an.nguyen@utc.edu.vn', '0901000001', NULL)",
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES ('RD002', 'Tran Thi Binh', 'STUDENT', DATE '2027-12-31', 15000, 'ACTIVE', 'binh.tran@utc.edu.vn', '0901000002', NULL)",
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES ('RD003', 'Le Hoang Cuong', 'STUDENT', DATE '2024-06-30', 0, 'EXPIRED', 'cuong.le@utc.edu.vn', '0901000003', NULL)",
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES ('RD004', 'Pham Minh Duc', 'LECTURER', DATE '2028-12-31', 0, 'ACTIVE', 'duc.pham@utc.edu.vn', '0901000004', NULL)",
            "INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status, email, phone, password_hash) "
                    + "VALUES ('RD005', 'Vo Thi Hoa', 'LECTURER', DATE '2028-06-30', 25000, 'ACTIVE', 'hoa.vo@utc.edu.vn', '0901000005', NULL)",

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

    public static void main(String[] args) {
        initialize();
        System.out.println(">>> [DB] DatabaseInitializer main() executed successfully!");
    }
}

