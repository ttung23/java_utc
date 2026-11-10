# 📖 HƯỚNG DẪN KHỞI CHẠY & BẢO VỆ DỰ ÁN SLMS
*(Smart Library Management System - Bài Tập Lớn Java UTC)*

---

## 🚀 1. Khởi chạy nhanh (Chỉ 1 Click)

Dự án đã được cấu hình sẵn **Java HttpServer** và **Local Database (Oracle Mode)** tự động khởi tạo bảng & dữ liệu mẫu ngay khi chạy lần đầu. Bạn **không cần phải cài đặt trước Oracle Database 3GB cồng kềnh** vẫn có thể chạy và demo toàn bộ tính năng ngay trên máy của mình!

### Cách 1: Chế độ Auto Rebuild & Live Reload (Khuyên dùng khi Code)
- Nhấp đúp chuột vào file **`dev.bat`**
- Server sẽ chạy và **liên tục lắng nghe mọi thay đổi trong thư mục `src/`**:
  - Khi bạn sửa file **Java**: Hệ thống tự động biên dịch lại (`javac`) và khởi động lại server trong 1 giây!
  - Khi bạn sửa file **HTML / CSS / JS**: Trình duyệt trên máy bạn sẽ **tự động refresh (F5)** cập nhật giao diện mới ngay lập tức mà bạn không cần phải bấm gì cả!

### Cách 2: Chạy bình thường bằng `run.bat`
- Nhấp đúp chuột vào file **`run.bat`**
- Server sẽ khởi động tại: **`http://localhost:8080`**

### Cách 2: Chạy bằng dòng lệnh Terminal / PowerShell
Mở PowerShell tại thư mục `baitaplon` và gõ:
```bash
mvn clean compile
mvn exec:java
```

Sau khi terminal hiện dòng:
```
>>> SERVER ĐANG CHẠY TẠI: http://localhost:8080
```
👉 Hãy mở trình duyệt (Chrome / Edge) và truy cập: **[http://localhost:8080](http://localhost:8080)**

---

## 🗄️ 2. Về Cơ Sở Dữ Liệu (Oracle Database)

Hệ thống được thiết kế theo đúng yêu cầu đề bài của Nhóm 12:
- Toàn bộ DDL bảng, khóa chính, khóa ngoại, chỉ mục và kiểu dữ liệu chuẩn Oracle (`VARCHAR2`, `NUMBER`, `DATE`, `TIMESTAMP`) nằm trong file:
  📁 **`database/schema.sql`**

### 🔹 Chế độ 1: Chạy Cục Bộ Ngay Lập Tức (Mặc định)
- Khi máy bạn chưa bật Oracle Database Server, lớp `DatabaseConnection.java` sẽ **tự động kích hoạt cơ chế Embedded Database (Oracle Compatibility Mode)** lưu tại `database/slms.mv.db`.
- Lớp `DatabaseInitializer.java` tự động nạp toàn bộ 6 bảng và dữ liệu mẫu (10 đầu sách, 15 bản sao, 5 độc giả, phiếu mượn, hóa đơn phạt).
- Bạn có thể thêm, sửa, xóa, mượn, trả sách thoải mái, dữ liệu được lưu bền vững vào file cục bộ.

### 🔹 Chế độ 2: Kết nối Oracle Database Thật (Khi nộp bài / demo phòng máy trường)
Nếu máy bạn hoặc phòng máy trường đã cài Oracle XE (11g/19c/21c):
1. Đăng nhập vào SQL*Plus hoặc DBeaver bằng tài khoản `sys as sysdba`.
2. Tạo người dùng và cấp quyền:
   ```sql
   CREATE USER slms_user IDENTIFIED BY slms_password;
   GRANT CONNECT, RESOURCE, DBA TO slms_user;
   ALTER USER slms_user QUOTA UNLIMITED ON USERS;
   ```
3. Chạy toàn bộ nội dung file `database/schema.sql` vào schema `slms_user`.
4. Khởi động ứng dụng, `DatabaseConnection.java` sẽ tự động ưu tiên kết nối Oracle thật qua cổng `1521` (SID: `xe`).

---

## 🎯 3. Kịch Bản Demo Bảo Vệ Bài Tập Lớn (Dành cho Thầy/Cô Chấm)

### 📌 Kịch bản 1: Lập Phiếu Mượn Sách (UC21)
1. Vào mục **Mượn / Trả Sách (UC)** trên thanh menu bên trái.
2. Chọn tab **"UC21: Lập Phiếu Mượn Sách"**.
3. **Thử nghiệm các ca kiểm thử nghiệp vụ (Business Rules):**
   - **Ca hợp lệ:** Nhập mã độc giả `RD001` (Sinh viên Nguyễn Văn An) -> Nhấn **"Kiểm Tra Thẻ"** -> Hệ thống báo hợp lệ -> Nhập mã vạch `BC-00001` -> Nhấn **"Xác Nhận Lập Phiếu Mượn"** -> Mượn thành công, hạn trả là 14 ngày.
   - **Ca thẻ hết hạn:** Nhập mã độc giả `RD003` (Lê Hoàng Cường) -> Nhấn **"Kiểm Tra Thẻ"** -> Bị chặn: *"Thẻ độc giả đã hết hạn sử dụng"*.
   - **Ca sách không sẵn sàng:** Nhập mã vạch `BC-00002` (sách đang được mượn) -> Báo lỗi: *"Bản sao không khả dụng (đang ở trạng thái Loaned)"*.

### 📌 Kịch bản 2: Trả Sách & Xử Lý Vi Phạm / Phạt (UC22)
1. Chuyển sang tab **"UC22: Nhận Trả Sách & Sự Cố"**.
2. **Ca trả sách quá hạn:**
   - Quét mã vạch: `BC-00005` (phiếu mượn `LN002` đã bị quá hạn từ tháng 10).
   - Chọn tình trạng: **Bình thường**.
   - Bấm **"Xác Nhận Nhận Trả Sách"**.
   - Hệ thống tự động tính số ngày trễ hạn, nhân với đơn giá **5.000 VNĐ/ngày**, sinh hóa đơn phạt `FINES` mới và cộng dồn nợ vào tài khoản độc giả!
3. **Ca trả sách bị hư hỏng / mất:**
   - Quét mã vạch `BC-00002`.
   - Chọn tình trạng: **Sách bị hư hỏng / rách nát** (nhập mức bồi thường 150%).
   - Bấm **"Xác Nhận"** -> Tự động tính tiền bồi thường theo giá bìa bản sao, chuyển trạng thái sách sang `Damaged`.

### 📌 Kịch bản 3: Thanh Toán Tiền Phạt
1. Vào mục **Hóa đơn Phạt** trên menu.
2. Xem danh sách các khoản phạt chưa thanh toán (Màu đỏ).
3. Bấm **"💵 Thu Tiền Phạt"** -> Khoản phạt chuyển sang trạng thái "Đã thanh toán" (Màu xanh), đồng thời số nợ của độc giả được trừ tương ứng.

### 📌 Kịch bản 4: Đặt Trước Sách (Reservation)
1. Vào mục **Đặt trước Sách** trên menu.
2. Thử đặt trước đầu sách đã hết sạch bản sao khả dụng trong kho -> Hệ thống tiếp nhận đơn đặt `Pending` và giữ chỗ trong 3 ngày khi sách được trả về kho.

---

## 🏛️ 4. Cấu Trúc Mã Nguồn (Tuân Thủ OOP & SOLID)

```
baitaplon/
├── pom.xml                               # Cấu hình Maven
├── run.bat                               # Script chạy 1-click
├── database/
│   └── schema.sql                        # Script DDL Oracle DB chuẩn + Sample Data
├── src/main/
│   ├── java/com/slms/
│   │   ├── App.java                      # Điểm khởi chạy HttpServer & DB
│   │   ├── model/                        # Domain Entities (Book, Reader, Loan, Fine...)
│   │   │   └── enums/                    # Enums trạng thái chuẩn
│   │   ├── dto/                          # DTO trao đổi dữ liệu (LoanDTO, ReturnResultDTO...)
│   │   ├── exception/                    # Các ngoại lệ nghiệp vụ (ReaderNotEligible...)
│   │   ├── repository/                   # Tầng truy xuất dữ liệu DAO (JDBC PreparedStatement)
│   │   ├── service/                      # Tầng nghiệp vụ Logic (LoanService, FineService...)
│   │   ├── server/                       # Tầng HTTP Handlers REST API
│   │   └── util/                         # Bộ sinh ID tự động, tiện ích Json
│   └── webapp/                           # Giao diện Web HTML / CSS / JS
│       ├── index.html                    # Dashboard tổng quan
│       ├── loans.html                    # Quầy Mượn/Trả UC21 & UC22
│       ├── books.html                    # Quản lý Sách & Mã vạch
│       ├── readers.html                  # Quản lý Độc giả
│       ├── fines.html                    # Quản lý Hóa đơn phạt
│       ├── reservations.html             # Đặt trước sách
│       ├── css/style.css                 # Giao diện phẳng, responsive
│       └── js/app.js                     # Xử lý API & Sidebar
```

