# QUY TẮC NGHIỆP VỤ VÀ YÊU CẦU HỆ THỐNG

## 1. QUY TẮC MƯỢN / TRẢ SÁCH (LOAN & RETURN RULES)
- **Hạn mức mượn sách theo loại độc giả:**
  - **Sinh viên (STUDENT):** Mượn tối đa **5 cuốn**, thời hạn mượn **14 ngày**.
  - **Giảng viên (LECTURER):** Mượn tối đa **10 cuốn**, thời hạn mượn **30 ngày**.
- **Điều kiện mượn sách của Độc giả (`canBorrow()`):**
  - Thẻ thư viện còn trong thời hạn sử dụng (`expiryDate >= current_date`).
  - Số lượng sách đang mượn hiện tại chưa vượt hạn mức tối đa quy định.
  - Tổng số tiền nợ phạt chưa thanh toán (`balanceFine`) **<= 50.000 VNĐ**.
- **Điều kiện của Bản sao sách (`BookCopy`):**
  - Trạng thái bản sao phải là `AVAILABLE`.
  - Nếu bản sao ở trạng thái `RESERVED`, chỉ độc giả đã tạo đơn đặt trước đó mới được phép mượn.

## 2. QUY TẮC PHẠT TRỄ HẠN & BỒI THƯỜNG (FINE RULES)
- **Phạt trễ hạn:** **5.000 VNĐ / ngày / cuốn** cho mỗi ngày trả quá hạn (`returnDate > dueDate`).
- **Phạt hỏng/mất sách:**
  - Sách bị hư hỏng hoặc làm mất: Độc giả phải bồi thường **100% đến 200%** giá trị bản sao sách (`price`).
  - Hệ thống tự động ghi nhận hóa đơn phạt (`FINE`) và cộng dồn vào `balanceFine` của độc giả cho đến khi thanh toán.

## 3. QUY TẮC ĐẶT TRƯỚC (RESERVATION RULES)
- Độc giả chỉ được đặt trước các Đầu sách (`BOOK`) hiện tại **không có bản sao nào khả dụng** (`AVAILABLE = 0`).
- Khi một bản sao được trả về:
  - Nếu có hàng chờ Đặt trước (`PENDING`), bản sao tự động chuyển sang trạng thái `RESERVED` cho độc giả đầu tiên trong hàng chờ.
  - Thời hạn giữ chỗ dành cho độc giả đặt trước là **3 ngày**. Quá 3 ngày không đến lấy, đơn đặt chuyển sang `EXPIRED` và bản sao chuyển về `AVAILABLE`.

## 4. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)
- **Hiệu năng (Performance):** Thời gian phản hồi quét mã vạch/kiểm tra tra cứu < 1.5 giây. Chịu tải đồng thời 500 người dùng.
- **Bảo mật (Security):** Mã hóa mật khẩu tài khoản bằng Bcrypt, phân quyền vai trò (RBAC: Admin, Librarian, Reader), ghi log kiểm toán giao dịch tài chính/phí phạt.
- **Độ sẵn sàng (Availability):** Khả năng vận hành 24/7 (99.5%), tự động sao lưu CSDL hàng ngày.
