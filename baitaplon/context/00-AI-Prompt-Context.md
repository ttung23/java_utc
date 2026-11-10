# SYSTEM PROMPT & CONTEXT HƯỚNG DẪN AI TẠO CODE (JAVA & ORACLE DB)

## 1. THÔNG TIN HỆ THỐNG
- **Tên dự án:** Hệ Thống Quản Lý Thư Viện Thông Minh (Smart Library Management System - SLMS).
- **Mục tiêu:** Tự động hóa toàn bộ quy trình mượn/trả sách, quản lý vòng đời bản sao vật lý, xử lý sự cố/quá hạn/phạt, đặt trước sách trực tuyến và báo cáo thống kê.
- **Tài liệu gốc:** Báo cáo Phân tích & Thiết kế Hướng đối tượng (OOAD) - Nhóm 12.

## 2. CÔNG NGHỆ & MÔ TRƯỜNG TRIỂN KHAI
- **Ngôn ngữ Backend:** Java 17/21 (Sử dụng Spring Boot Framework hoặc Pure Java OOP + JDBC/JPA).
- **Cơ sở dữ liệu:** Oracle Database (Oracle SQL / PL-SQL).
- **Kiến trúc phần mềm:** Kiến trúc 3 tầng (3-Tier Layered Architecture):
  - **Presentation Layer (Boundary):** RESTful Controller / CLI / Web & Scanner App.
  - **Business Logic Layer (Control & Service):** Services (`LoanService`, `FineService`, `ReservationService`, `BookService`).
  - **Persistence Layer (Repository / DAO):** Spring Data JPA hoặc JDBC Repositories kết nối Oracle DB.

## 3. QUY TẮC LẬP TRÌNH (CODING STANDARDS FOR AI)
1. **Tuân thủ Nguyên lý OOP & SOLID:**
   - Phân tách rõ ràng giữa Entity (Domain Object), DTO, Controller, Service và Repository.
   - Sử dụng Encapsulation (Private fields + Getters/Setters), Polymorphism và Interface Segregation.
2. **Quy tắc Đặt tên (Java & Oracle DB):**
   - **Java:** CamelCase cho Class (`BookCopy`, `LoanService`), camelCase cho variable/method (`borrowDate`, `canBorrow()`), UPPER_SNAKE_CASE cho Constant/Enum (`LOANED`, `AVAILABLE`).
   - **Oracle DB:** UPPER_SNAKE_CASE cho bảng (`BOOKS`, `BOOK_COPIES`), cột (`BOOK_ID`, `CREATE_DATE`), kiểu dữ liệu Oracle chuẩn (`VARCHAR2`, `NUMBER`, `TIMESTAMP`, `DATE`).
3. **Xử lý Ngoại lệ (Exception Handling):**
   - Định nghĩa các Custom Exception: `ReaderNotEligibleException`, `BookNotAvailableException`, `MaxBorrowLimitExceededException`, `FineExceededException`.
   - Service layer bắt lỗi nghiệp vụ và trả về thông điệp rõ ràng.
4. **Quản lý Giao dịch (Transaction Management):**
   - Mọi thao tác ghi/sửa dữ liệu phức tạp (như Mượn sách = Tạo Loan + Đổi trạng thái BookCopy) phải bọc trong Transaction (`@Transactional` trong Spring Boot hoặc explicit Commit/Rollback JDBC).
