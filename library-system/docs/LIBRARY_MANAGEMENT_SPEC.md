# 📚 ĐẶC TẢ NGHIỆP VỤ: HỆ THỐNG QUẢN LÝ THƯ VIỆN (LIBRARY MANAGEMENT SYSTEM)

> Tài liệu dành cho Cursor/AI code assistant. Đọc toàn bộ trước khi code.
> Hệ thống gồm **2 ứng dụng giao diện**: **Trang Admin/Quản trị (nội bộ)** và **Trang Client (độc giả)**, dùng chung 1 backend + 1 database.
> Quy ước: `BR-xx` = Business Rule (quy tắc nghiệp vụ bắt buộc), `FR-xx` = Functional Requirement, `[ADMIN]` = chỉ Admin, `[STAFF]` = Nhân viên (thủ thư) trở lên, `[USER]` = Độc giả.

---

## MỤC LỤC

1. Tổng quan & phạm vi
2. Công nghệ đề xuất & cấu trúc thư mục
3. Vai trò & phân quyền (RBAC)
4. Danh sách module
5. Quy tắc nghiệp vụ cốt lõi (cấu hình mượn/trả/phạt)
6. Nghiệp vụ chi tiết từng module
7. NGHIỆP VỤ PHIẾU MƯỢN SÁCH (trọng tâm)
8. Thiết kế Database
9. Thiết kế API
10. Giao diện Admin (từng trang)
11. Giao diện Client (từng trang)
12. Thông báo, báo cáo, nhật ký
13. Phi chức năng (bảo mật, hiệu năng, UX)
14. Dữ liệu mẫu (seed) & test case
15. Lộ trình triển khai cho Cursor (prompt theo phase)

---

## 1. TỔNG QUAN & PHẠM VI

### 1.1 Mục tiêu
Số hóa toàn bộ hoạt động thư viện: quản lý đầu sách và từng bản sao vật lý (barcode), độc giả/thẻ thư viện, mượn – trả – gia hạn – đặt trước, phạt, nhập kho, kiểm kê, thống kê; đồng thời cho độc giả tra cứu, đặt mượn, theo dõi lịch sử qua web.

### 1.2 Đối tượng sử dụng
| Vai trò | Mô tả | Giao diện |
|---|---|---|
| **ADMIN** | Quản trị hệ thống, toàn quyền: nhân sự, cấu hình, báo cáo, nhật ký | Trang Admin |
| **STAFF (LIBRARIAN)** | Nhân viên/thủ thư: nghiệp vụ hằng ngày (cho mượn, nhận trả, thu phạt, quản lý sách, độc giả) | Trang Admin (menu bị giới hạn) |
| **USER (READER)** | Độc giả có tài khoản: tìm sách, đặt mượn online, gia hạn, xem lịch sử, nộp phạt | Trang Client |
| **GUEST** | Khách chưa đăng nhập: chỉ tra cứu sách, xem tin tức | Trang Client |

### 1.3 Phạm vi (In scope)
Quản lý sách (đầu sách + bản sao), tác giả/NXB/thể loại/vị trí kệ, độc giả & thẻ, mượn/trả/gia hạn/đặt trước, phạt & thu tiền, mất/hỏng sách, nhập sách & nhà cung cấp, kiểm kê, thông báo, báo cáo/thống kê, phân quyền, nhật ký hệ thống, cấu hình chính sách, tin tức/thông báo thư viện, đánh giá/yêu thích sách.

### 1.4 Ngoài phạm vi (Phase sau)
Thanh toán online thật (chỉ mô phỏng/ghi nhận), sách điện tử DRM, tích hợp RFID, mobile app native.

---

## 2. CÔNG NGHỆ ĐỀ XUẤT & CẤU TRÚC THƯ MỤC

> Có thể thay đổi stack theo ý bạn, nhưng giữ nguyên kiến trúc & nghiệp vụ.

- **Frontend (2 app)**: React + TypeScript + Vite (hoặc Next.js), TailwindCSS, shadcn/ui hoặc Ant Design, React Query, React Hook Form + Zod, React Router, Recharts.
- **Backend**: Node.js + NestJS (hoặc Express) + TypeScript, Prisma ORM / TypeORM.
- **Database**: PostgreSQL (hoặc MySQL).
- **Auth**: JWT (access 15 phút + refresh 7 ngày), bcrypt, RBAC guard.
- **Khác**: Redis (cache/queue tùy chọn), node-cron (job quá hạn/hết hạn giữ sách), Nodemailer (email), multer (upload ảnh bìa), bwip-js/JsBarcode (barcode), PDFKit/React-to-print (in phiếu).

```
library-system/
├── apps/
│   ├── admin-web/            # Giao diện Admin + Staff
│   │   └── src/{pages,components,features,hooks,services,store,routes,utils}
│   ├── client-web/           # Giao diện Độc giả
│   │   └── src/{pages,components,features,hooks,services,store,routes,utils}
│   └── api/                  # Backend
│       └── src/{modules,common,config,database,jobs}
│           └── modules/{auth,users,readers,cards,books,copies,authors,publishers,categories,shelves,
│                        borrows,reservations,fines,payments,imports,suppliers,inventory,
│                        notifications,reports,settings,audit,news,reviews}
├── packages/shared/          # types, enums, zod schema dùng chung
├── docs/
└── README.md
```

---

## 3. VAI TRÒ & PHÂN QUYỀN (RBAC)

### 3.1 Ma trận quyền chi tiết

Ký hiệu: ✅ toàn quyền | 👁 chỉ xem | ➕ tạo | ✏️ sửa | 🗑 xóa | ❌ không

| Chức năng | GUEST | USER | STAFF | ADMIN |
|---|:-:|:-:|:-:|:-:|
| Tra cứu/xem chi tiết sách | 👁 | 👁 | ✅ | ✅ |
| Đăng ký/đăng nhập/quên mật khẩu | ✅ | ✅ | ✅ | ✅ |
| Quản lý hồ sơ cá nhân | ❌ | ✏️ (của mình) | ✏️ (của mình) | ✏️ |
| Đặt mượn online (tạo yêu cầu mượn) | ❌ | ➕ | ➕ (thay độc giả) | ➕ |
| Xem/hủy/gia hạn phiếu mượn của mình | ❌ | ✅ (của mình) | ✅ (tất cả) | ✅ |
| Đặt trước sách (reservation) | ❌ | ➕🗑 (của mình) | ✅ | ✅ |
| Yêu thích / đánh giá sách | ❌ | ✅ | 👁 | ✅ (duyệt/ẩn) |
| **Tạo phiếu mượn tại quầy** | ❌ | ❌ | ➕ | ➕ |
| Duyệt/từ chối yêu cầu mượn online | ❌ | ❌ | ✅ | ✅ |
| Giao sách (xác nhận lấy sách) | ❌ | ❌ | ✅ | ✅ |
| **Nhận trả sách** | ❌ | ❌ | ✅ | ✅ |
| Gia hạn thay độc giả | ❌ | ❌ | ✅ | ✅ |
| Miễn/giảm phạt | ❌ | ❌ | ❌ (đề xuất) | ✅ |
| Thu tiền phạt | ❌ | ❌ | ✅ | ✅ |
| Quản lý đầu sách | ❌ | ❌ | ➕✏️ | ➕✏️🗑 |
| Quản lý bản sao (copy) & barcode | ❌ | ❌ | ➕✏️ | ✅ |
| Quản lý tác giả/NXB/thể loại/kệ | ❌ | ❌ | ➕✏️ | ✅ |
| Quản lý độc giả & thẻ | ❌ | ❌ | ➕✏️ (khóa tạm) | ✅ |
| Nhập sách / nhà cung cấp | ❌ | ❌ | ➕ (tạo phiếu nhập) | ✅ (duyệt) |
| Kiểm kê | ❌ | ❌ | ➕✏️ | ✅ (chốt, điều chỉnh) |
| Thanh lý/loại bỏ sách | ❌ | ❌ | ❌ (đề xuất) | ✅ |
| Báo cáo & thống kê | ❌ | ❌ | 👁 (báo cáo vận hành) | ✅ (toàn bộ) |
| Quản lý nhân viên & phân quyền | ❌ | ❌ | ❌ | ✅ |
| Cấu hình chính sách thư viện | ❌ | ❌ | 👁 | ✅ |
| Nhật ký hệ thống (audit log) | ❌ | ❌ | ❌ | 👁 |
| Quản lý tin tức/thông báo chung | ❌ | ❌ | ➕✏️ | ✅ |

### 3.2 Yêu cầu kỹ thuật phân quyền
- Backend: `@Roles('ADMIN','STAFF')` guard trên mọi endpoint; USER chỉ truy cập dữ liệu **của chính mình** (kiểm tra `reader_id = currentUser.readerId`).
- Frontend: Route guard theo role; ẩn menu/nút theo quyền; `403` page.
- Admin app: nếu USER đăng nhập → từ chối. Client app: STAFF/ADMIN vẫn đăng nhập được (xem như user thường) nhưng có nút "Vào trang quản trị".
- Nên triển khai permission mịn (`permission code`, ví dụ `borrow.create`, `fine.waive`) + bảng `role_permissions` để ADMIN tùy chỉnh quyền STAFF (Phase nâng cao). Phase 1 dùng enum role cứng theo ma trận trên.

---

## 4. DANH SÁCH MODULE

| # | Module | Admin | Client |
|---|---|:-:|:-:|
| M1 | Xác thực & tài khoản | ✅ | ✅ |
| M2 | Quản lý nhân viên & phân quyền | ✅ | |
| M3 | Quản lý độc giả & thẻ thư viện | ✅ | ✅ (hồ sơ) |
| M4 | Danh mục: thể loại, tác giả, NXB, kệ/vị trí, ngôn ngữ | ✅ | |
| M5 | Quản lý đầu sách (Book/Title) | ✅ | ✅ (xem) |
| M6 | Quản lý bản sao (Book Copy – barcode) | ✅ | |
| M7 | **Mượn sách – Phiếu mượn** | ✅ | ✅ |
| M8 | **Trả sách** | ✅ | |
| M9 | Gia hạn | ✅ | ✅ |
| M10 | Đặt trước (Reservation) | ✅ | ✅ |
| M11 | Phạt & thu tiền (Fine/Payment) | ✅ | ✅ (xem/nộp) |
| M12 | Mất/hỏng sách | ✅ | |
| M13 | Nhà cung cấp & Nhập sách | ✅ | |
| M14 | Kiểm kê kho | ✅ | |
| M15 | Thanh lý/loại bỏ | ✅ | |
| M16 | Thông báo (email + in-app) | ✅ | ✅ |
| M17 | Báo cáo & thống kê, dashboard | ✅ | |
| M18 | Cấu hình chính sách | ✅ | |
| M19 | Audit log | ✅ | |
| M20 | Tin tức/Thông báo thư viện/Banner | ✅ | ✅ |
| M21 | Đánh giá, yêu thích, gợi ý sách | ✅ (duyệt) | ✅ |
| M22 | Tra cứu nâng cao & trang chủ | | ✅ |

---

## 5. QUY TẮC NGHIỆP VỤ CỐT LÕI (CẤU HÌNH ĐƯỢC TRONG ADMIN → `settings`)

> Tất cả số liệu dưới đây là **giá trị mặc định**, phải lưu trong bảng `settings` / `reader_types` để Admin chỉnh được, **KHÔNG hard-code**.

### 5.1 Loại độc giả (reader_types) – giới hạn mượn khác nhau
| Loại | Mã | Số sách tối đa cùng lúc | Số ngày mượn | Số lần gia hạn | Ngày/lần gia hạn | Số sách đặt trước tối đa | Phí làm thẻ | Hạn thẻ |
|---|---|:-:|:-:|:-:|:-:|:-:|---|---|
| Sinh viên | STUDENT | 5 | 14 | 2 | 7 | 3 | 20.000đ | 12 tháng |
| Học sinh | PUPIL | 3 | 14 | 1 | 7 | 2 | 10.000đ | 12 tháng |
| Giảng viên/Cán bộ | TEACHER | 10 | 30 | 3 | 14 | 5 | 0đ | 24 tháng |
| Khách vãng lai/Cộng đồng | PUBLIC | 3 | 10 | 1 | 5 | 2 | 50.000đ | 12 tháng |
| VIP/Thành viên đặc biệt | VIP | 15 | 45 | 3 | 15 | 8 | 100.000đ | 24 tháng |

### 5.2 Quy tắc mượn (Business Rules)
- **BR-01**: Độc giả phải có **thẻ ACTIVE** và **chưa hết hạn** tại thời điểm mượn.
- **BR-02**: Không được mượn nếu tài khoản bị **BLOCKED/SUSPENDED**.
- **BR-03**: Không được mượn nếu đang có **sách quá hạn chưa trả** (cấu hình `block_borrow_if_overdue = true`).
- **BR-04**: Không được mượn nếu **tổng tiền phạt chưa nộp > ngưỡng** (mặc định 50.000đ; cấu hình `max_unpaid_fine`).
- **BR-05**: Tổng số sách đang mượn (status BORROWING/OVERDUE) + số sách trong phiếu mới ≤ `max_books` của loại độc giả.
- **BR-06**: **Không mượn trùng**: 1 độc giả không được mượn 2 bản sao của cùng 1 đầu sách cùng lúc.
- **BR-07**: Chỉ mượn **bản sao AVAILABLE** và đầu sách có `loanable = true`. Sách **"Chỉ đọc tại chỗ"** (`is_reference = true`) không cho mượn về.
- **BR-08**: Sách đang **HOLD** (giữ cho người đặt trước) chỉ cho đúng người được giữ mượn.
- **BR-09**: Ngày hẹn trả (`due_date`) = ngày mượn + số ngày theo loại độc giả (có thể ghi đè theo đầu sách: `loan_days_override`, ví dụ sách hot chỉ 7 ngày). Nếu `due_date` rơi vào **ngày nghỉ/lễ** (bảng `holidays`) hoặc **Chủ nhật** → tự động dời sang ngày làm việc kế tiếp.
- **BR-10**: Một phiếu mượn có thể có **nhiều sách (items)**; mỗi item theo dõi trạng thái, hạn trả, gia hạn **riêng**.
- **BR-11**: Một phiếu mượn tối đa `max_items_per_slip` (mặc định = max_books).
- **BR-12**: Nếu độc giả đang có yêu cầu mượn online PENDING/APPROVED, số sách đó **tính vào hạn mức** (tránh vượt hạn mức khi gửi nhiều yêu cầu).

### 5.3 Quy tắc mượn online – giữ sách
- **BR-13**: Yêu cầu online → trạng thái `PENDING`. Staff duyệt → `APPROVED` và hệ thống **giữ (HOLD) các bản sao** trong `hold_hours` (mặc định 48 giờ).
- **BR-14**: Quá hạn lấy sách mà không đến → job tự động chuyển `EXPIRED`, nhả bản sao về `AVAILABLE` (hoặc chuyển cho người đặt trước kế tiếp), ghi nhận **vi phạm không đến lấy** (cộng điểm no-show; ≥ 3 lần/6 tháng → khóa mượn online 30 ngày – cấu hình được).
- **BR-15**: Độc giả có thể **hủy** yêu cầu khi còn `PENDING`/`APPROVED`.

### 5.4 Gia hạn
- **BR-16**: Gia hạn theo **từng item**; số lần ≤ `max_renewals` của loại độc giả.
- **BR-17**: **Không gia hạn** nếu: item đã quá hạn; sách có người đặt trước (reservation WAITING); độc giả đang bị phạt vượt ngưỡng; thẻ hết hạn trước ngày hạn mới.
- **BR-18**: Hạn mới = `due_date hiện tại` + `renew_days` (không tính từ ngày bấm). Áp dụng quy tắc ngày nghỉ BR-09.
- **BR-19**: Mỗi lần gia hạn ghi vào bảng `renewals` (ai gia hạn: user online hay staff).

### 5.5 Đặt trước (Reservation)
- **BR-20**: Chỉ đặt trước khi **tất cả bản sao đang BORROWED/HOLD** (không còn AVAILABLE). Còn sách rảnh → hướng dẫn mượn trực tiếp.
- **BR-21**: Hàng đợi **FIFO** theo `created_at`; mỗi đầu sách 1 hàng đợi.
- **BR-22**: Khi có bản sao được trả → bản sao chuyển `HOLD`, reservation đầu hàng chuyển `READY`, `ready_until = now + hold_hours`, gửi thông báo. Quá hạn không nhận → `EXPIRED`, chuyển cho người kế tiếp.
- **BR-23**: Không đặt trước nếu đang mượn đầu sách đó hoặc đã đặt trước đầu sách đó (WAITING/READY).
- **BR-24**: Số đặt trước đồng thời ≤ `max_reservations` theo loại độc giả.

### 5.6 Phạt (Fine)
| Loại phạt | Mã | Công thức mặc định |
|---|---|---|
| Trả trễ | `OVERDUE` | `số ngày trễ × overdue_fee_per_day` (mặc định **2.000đ/ngày/cuốn**), tối đa = **giá bìa sách** (cap), có `grace_days` (mặc định 0) |
| Làm hỏng nhẹ | `DAMAGED_MINOR` | 20% giá bìa (cấu hình) |
| Làm hỏng nặng | `DAMAGED_MAJOR` | 60% giá bìa |
| Mất sách | `LOST` | `giá bìa × 2` (hoặc `giá bìa + phí xử lý 20.000đ`) – cấu hình |
| Mất thẻ/làm lại thẻ | `CARD_REISSUE` | 20.000đ |
| Không đến lấy sách (no-show) | `NO_SHOW` | 0đ (chỉ tính điểm), tùy chọn cấu hình |

- **BR-25**: Ngày trễ tính theo **ngày dương lịch**, có thể cấu hình **bỏ qua ngày nghỉ/lễ** (`skip_holidays_in_fine`).
- **BR-26**: Phạt được tạo **khi trả sách** (hoặc khi đánh dấu mất/hỏng); hệ thống cũng hiển thị **phạt dự kiến** realtime cho sách chưa trả.
- **BR-27**: Phạt có trạng thái `UNPAID → PARTIAL → PAID` hoặc `WAIVED` (miễn – chỉ ADMIN, bắt buộc ghi lý do) hoặc `REDUCED`.
- **BR-28**: Thu phạt tạo `payments` (tiền mặt/chuyển khoản/ví) có số biên lai `BL-YYYYMMDD-xxxx`; cho phép thu 1 phần.
- **BR-29**: Sách **mất** được độc giả đền sách thay thế (đúng đầu sách) → Admin duyệt miễn tiền, copy cũ ghi `LOST`, copy mới nhập vào kho.
- **BR-30**: Khi phát hiện hư hỏng lúc trả, bắt buộc staff nhập **mô tả + (tùy chọn) ảnh** minh chứng.

### 5.7 Thẻ thư viện
- **BR-31**: Mã thẻ duy nhất `TV-YYYY-000001`, có barcode/QR để quét tại quầy.
- **BR-32**: Trạng thái thẻ: `PENDING` (chờ duyệt đăng ký online), `ACTIVE`, `EXPIRED`, `SUSPENDED` (khóa tạm), `LOST`, `CANCELLED`.
- **BR-33**: Gia hạn thẻ: thu phí theo loại độc giả, cộng thêm thời hạn; thẻ hết hạn vẫn xem lịch sử nhưng không mượn.
- **BR-34**: Báo mất thẻ → khóa thẻ cũ ngay, cấp thẻ mới (mã thẻ mới, giữ nguyên lịch sử mượn), thu phí `CARD_REISSUE`.
- **BR-35**: Độc giả còn sách đang mượn/phạt chưa nộp thì **không được hủy thẻ**.

### 5.8 Bản sao sách (Copy)
- Trạng thái: `AVAILABLE`, `BORROWED`, `HOLD`, `LOST`, `DAMAGED`, `MAINTENANCE`, `WITHDRAWN` (đã thanh lý), `IN_TRANSIT` (đang chuyển kệ).
- Tình trạng vật lý (`condition`): `NEW`, `GOOD`, `FAIR`, `POOR`.
- **BR-36**: Mỗi bản sao có **barcode duy nhất** `BC-<mã đầu sách>-<số thứ tự>` (hoặc theo chuẩn tự sinh). Không xóa cứng copy đã có lịch sử mượn – chỉ chuyển `WITHDRAWN`.
- **BR-37**: Số lượng của đầu sách (`total_copies`, `available_copies`) là **giá trị tính toán**, cập nhật tự động hoặc truy vấn (không nhập tay).

### 5.9 Cấu hình chung (bảng settings)
`library_name`, `address`, `phone`, `email`, `open_hours`, `weekend_days`, `hold_hours` (48), `overdue_fee_per_day` (2000), `grace_days` (0), `max_unpaid_fine` (50000), `block_borrow_if_overdue` (true), `due_soon_reminder_days` (2), `overdue_reminder_interval_days` (3), `lost_book_multiplier` (2), `lost_processing_fee` (20000), `no_show_limit` (3), `allow_online_borrow` (true), `allow_self_renew` (true), `default_currency` (VND), `date_format` (DD/MM/YYYY), `fine_cap_by_price` (true), `card_validity_months`.

---

## 6. NGHIỆP VỤ CHI TIẾT TỪNG MODULE

### M1. Xác thực & tài khoản
- **FR-1.1** Đăng ký (client): họ tên, email, SĐT, mật khẩu, loại độc giả, ngày sinh, địa chỉ, số CCCD/MSSV, ảnh chân dung (tùy chọn). Sau đăng ký: tài khoản `PENDING` → staff duyệt & cấp thẻ (hoặc auto-approve nếu cấu hình). Xác thực email bằng link/OTP.
- **FR-1.2** Đăng nhập bằng email/SĐT/mã thẻ + mật khẩu. Khóa 15 phút sau 5 lần sai.
- **FR-1.3** Quên mật khẩu (link reset 30 phút), đổi mật khẩu (yêu cầu mật khẩu cũ).
- **FR-1.4** Refresh token, đăng xuất, đăng xuất mọi thiết bị.
- **FR-1.5** Admin có thể reset mật khẩu, khóa/mở khóa tài khoản.
- Mật khẩu ≥ 8 ký tự, có chữ hoa, thường, số.

### M2. Quản lý nhân viên [ADMIN]
- CRUD nhân viên: mã NV, họ tên, email, SĐT, vai trò (STAFF/ADMIN), trạng thái (ACTIVE/INACTIVE), ca làm việc.
- Không xóa cứng NV đã phát sinh giao dịch → chỉ `INACTIVE`.
- Xem lịch sử thao tác của từng nhân viên (từ audit log).
- Không cho Admin tự hạ quyền/khóa chính mình; luôn còn ≥ 1 Admin ACTIVE.

### M3. Quản lý độc giả & thẻ [STAFF+]
- Danh sách: tìm theo tên/mã thẻ/SĐT/email/CCCD; lọc theo loại, trạng thái thẻ, sắp hết hạn, có nợ phạt, đang quá hạn.
- Thêm độc giả tại quầy (đăng ký trực tiếp) → tự tạo user + thẻ + in thẻ (barcode).
- Hồ sơ độc giả (tab): Thông tin | Thẻ | Đang mượn | Lịch sử mượn | Đặt trước | Phạt & thanh toán | Ghi chú nội bộ.
- Hành động: khóa/mở khóa tạm (kèm lý do), gia hạn thẻ, báo mất & cấp lại thẻ, đổi loại độc giả, import Excel hàng loạt (sinh viên đầu năm), export.
- Danh sách đen: độc giả vi phạm nghiêm trọng (`blacklisted = true`) bị chặn mượn.

### M4. Danh mục
- **Thể loại (categories)**: dạng cây (cha-con), mã phân loại (DDC/tùy chọn), mô tả.
- **Tác giả**: tên, bút danh, quốc tịch, năm sinh/mất, tiểu sử, ảnh.
- **Nhà xuất bản**: tên, địa chỉ, SĐT, email, website.
- **Kệ/Vị trí (shelves)**: khu vực (Tầng/Phòng) → Dãy → Kệ → Ngăn; mã vị trí `A1-K03-N2`; sức chứa.
- **Ngôn ngữ**, **Loại tài liệu** (Sách, Giáo trình, Tạp chí, Luận văn, Báo, CD/DVD…).
- Không xóa danh mục đang được tham chiếu (chặn + gợi ý chuyển/gộp). Có chức năng **gộp** tác giả/NXB trùng.

### M5. Đầu sách (Book/Title)
Trường: ISBN-10/13, tên sách, tên gốc/phụ đề, tác giả (nhiều), NXB, năm xuất bản, lần tái bản, thể loại (nhiều), ngôn ngữ, số trang, khổ sách, **giá bìa**, mô tả/tóm tắt, ảnh bìa, từ khóa/tag, loại tài liệu, mã phân loại, `is_reference` (chỉ đọc tại chỗ), `loanable`, `loan_days_override`, trạng thái (`ACTIVE/HIDDEN`), ngày nhập đầu tiên.
- **FR-5.1** CRUD, tìm kiếm full-text theo tên/tác giả/ISBN/tag.
- **FR-5.2** Kiểm tra trùng ISBN khi thêm (cảnh báo → gợi ý "thêm bản sao" thay vì tạo đầu sách mới).
- **FR-5.3** Upload ảnh bìa (≤ 2MB, jpg/png/webp), tự resize thumbnail.
- **FR-5.4** Import Excel/CSV hàng loạt (có preview + báo lỗi từng dòng); export.
- **FR-5.5** (Tùy chọn) Tra ISBN tự điền thông tin từ Google Books/OpenLibrary API.
- Hiển thị tóm tắt tồn: Tổng bản | Có sẵn | Đang mượn | Giữ chỗ | Mất/Hỏng.
- Ẩn (HIDDEN) thay vì xóa; chỉ xóa cứng khi chưa có bản sao.

### M6. Bản sao (Copy)
- Khi thêm sách (hoặc nhập kho) nhập **số lượng bản sao** → hệ thống sinh barcode hàng loạt, gán vị trí kệ, tình trạng, giá nhập, ngày nhập, nguồn nhập (mua/tặng/…).
- In tem barcode hàng loạt (PDF lưới tem).
- Chuyển kệ, đổi tình trạng, đánh dấu bảo trì, thanh lý.
- Xem lịch sử vòng đời từng bản sao (nhập → mượn → trả → hỏng…).
- Quét barcode ở mọi ô tìm kiếm nghiệp vụ (máy quét USB hoạt động như bàn phím + Enter).

### M11. Thu tiền phạt
- Danh sách khoản phạt theo độc giả; chọn nhiều khoản → thu → chọn phương thức (Tiền mặt/Chuyển khoản/Ví) → tạo biên lai, in.
- Thu một phần, ghi nợ còn lại.
- **Miễn/giảm** [ADMIN]: bắt buộc lý do, lưu log.
- Hoàn tiền (refund) khi thu nhầm [ADMIN].
- Phía client: xem khoản phạt, "nộp phạt" (Phase 1: tạo yêu cầu thanh toán chờ staff xác nhận; Phase 2: cổng thanh toán).

### M13. Nhà cung cấp & Nhập sách [STAFF tạo, ADMIN duyệt]
- Nhà cung cấp: tên, người liên hệ, SĐT, email, địa chỉ, MST, ghi chú, trạng thái.
- **Phiếu nhập** `PN-YYYYMMDD-xxxx`: nhà cung cấp, ngày nhập, nguồn (MUA/TẶNG/CHUYỂN KHO/ĐỀN BÙ), chi tiết (đầu sách, số lượng, đơn giá, vị trí kệ), tổng tiền, trạng thái `DRAFT → PENDING_APPROVAL → COMPLETED/CANCELLED`.
- Khi `COMPLETED` → tự sinh bản sao + barcode cho từng dòng.
- (Tùy chọn) **Đề xuất mua sách**: độc giả gửi gợi ý sách thư viện chưa có → staff xem xét.

### M14. Kiểm kê
- Tạo đợt kiểm kê (`KK-YYYY-xx`): phạm vi (toàn kho/theo kệ/theo thể loại), ngày bắt đầu, người phụ trách.
- Quét barcode từng bản sao trên kệ (hoặc nhập tay) → đánh dấu "Đã thấy".
- Kết thúc: hệ thống đối chiếu **Sổ sách vs Thực tế** → danh sách: ✅ Khớp, ❌ Thiếu (không thấy), ⚠️ Thừa/sai vị trí.
- Thiếu: staff đề xuất chuyển `LOST`; Admin duyệt → điều chỉnh trạng thái, ghi log. Xuất biên bản kiểm kê PDF/Excel.

### M15. Thanh lý/Loại bỏ [ADMIN]
- Phiếu thanh lý: chọn bản sao (hỏng nặng/lạc hậu/mất) + lý do → Admin duyệt → copy chuyển `WITHDRAWN`, không còn tính vào tồn.

### M16. Thông báo
Kênh: **in-app** (chuông) + **email** (+ SMS tùy chọn). Template cấu hình được (biến `{{reader_name}}`, `{{book_title}}`, `{{due_date}}`…).
| Sự kiện | Người nhận |
|---|---|
| Đăng ký thành công/chờ duyệt/được duyệt | USER |
| Yêu cầu mượn được duyệt/từ chối/sắp hết hạn giữ | USER |
| Mượn thành công (kèm phiếu) | USER |
| Sắp đến hạn trả (T-2 ngày) | USER |
| Quá hạn (nhắc định kỳ mỗi 3 ngày) | USER (+ staff dashboard) |
| Gia hạn thành công/bị từ chối | USER |
| Sách đặt trước đã sẵn sàng | USER |
| Phát sinh phạt, nhắc nộp phạt | USER |
| Thẻ sắp hết hạn (T-30, T-7) | USER |
| Có yêu cầu mượn mới, sách quá hạn nhiều, tồn kho thấp | STAFF/ADMIN |

### M20. Tin tức/Thông báo/Banner
- CRUD bài viết (tiêu đề, ảnh, nội dung rich text, trạng thái nháp/đăng, ngày đăng, ghim), banner trang chủ client, thông báo nghỉ lễ (đồng bộ vào bảng `holidays`), nội quy thư viện (trang tĩnh).

### M21. Đánh giá & yêu thích
- USER: yêu thích (wishlist), đánh giá 1–5 sao + bình luận (chỉ người **đã mượn** đầu sách đó mới được đánh giá), mỗi người 1 đánh giá/đầu sách (sửa được).
- STAFF/ADMIN: duyệt/ẩn đánh giá vi phạm.
- Gợi ý: "Sách mới", "Mượn nhiều nhất", "Cùng tác giả/thể loại", "Có thể bạn thích".

---

## 7. ⭐ NGHIỆP VỤ PHIẾU MƯỢN SÁCH (TRỌNG TÂM)

### 7.1 Hai kênh tạo phiếu mượn
1. **Mượn tại quầy (Counter)** — STAFF tạo trực tiếp, hoàn tất ngay → trạng thái `BORROWING`.
2. **Mượn online (Online request)** — USER chọn sách trên client, gửi yêu cầu → STAFF duyệt → giữ sách → USER đến quầy nhận → STAFF xác nhận giao.

### 7.2 Mã phiếu & cấu trúc
- Mã phiếu: `PM-YYYYMMDD-NNNN` (reset số thứ tự mỗi ngày). Mã yêu cầu online dùng chung mã này.
- **Phiếu mượn (header)**: mã phiếu, độc giả, kênh (`COUNTER/ONLINE`), nhân viên lập, nhân viên duyệt, ngày tạo, ngày duyệt, ngày giao (borrow_date), hạn lấy sách (`pickup_deadline`), ghi chú, trạng thái phiếu.
- **Chi tiết (items)**: copy_id, book_id, ngày mượn, hạn trả (`due_date`), số lần gia hạn, ngày trả thực tế, trạng thái item, tình trạng lúc mượn, tình trạng lúc trả, phạt phát sinh.

### 7.3 Máy trạng thái PHIẾU (slip.status)
```
              (online)                         (online)
 [PENDING] ──duyệt──▶ [APPROVED] ──giao sách──▶ [BORROWING] ──trả hết──▶ [RETURNED]
    │  │                  │  │                      │  ▲                     
    │  └─từ chối─▶[REJECTED]  └─quá hạn lấy─▶[EXPIRED]   │ trả một phần          
    └─user hủy─▶[CANCELLED]   └─user hủy─▶[CANCELLED]    ▼                       
                                                   [PARTIALLY_RETURNED] ──trả hết──▶ [RETURNED]
 (counter) ──tạo & xác nhận──▶ [BORROWING]
```
- `OVERDUE` **không phải status lưu cứng** mà là cờ tính toán: item có `status = BORROWING` và `due_date < today`. Phiếu hiển thị badge "Quá hạn" nếu có ≥ 1 item quá hạn. (Có thể cache bằng job hằng ngày.)
- Phiếu `RETURNED` khi **tất cả** item đã `RETURNED/LOST`.

### 7.4 Máy trạng thái ITEM (item.status)
`RESERVED_HOLD` (đã duyệt, đang giữ) → `BORROWING` → `RETURNED` | `LOST` | `DAMAGED_RETURNED`; nhánh: `CANCELLED`/`EXPIRED` (khi phiếu hủy/hết hạn lấy). Cờ phụ: `renew_count`.

### 7.5 LUỒNG A — Tạo phiếu mượn tại quầy (STAFF) — chi tiết từng bước

**Màn hình: "Tạo phiếu mượn" (layout 2 cột)**

Bước 1 — **Chọn độc giả**
- Ô tìm kiếm/quét **mã thẻ** (autofocus), hoặc tìm theo tên/SĐT (dropdown gợi ý).
- Sau khi chọn → hiển thị **Thẻ thông tin độc giả**: ảnh, họ tên, mã thẻ, loại, hạn thẻ, trạng thái thẻ, **số sách đang mượn / tối đa**, số sách quá hạn, tổng phạt chưa nộp, số đặt trước đang READY.
- Chạy **kiểm tra điều kiện (BR-01→BR-05)** và hiển thị:
  - 🟢 Đủ điều kiện mượn (còn N lượt mượn).
  - 🔴 Không đủ điều kiện + **lý do cụ thể** (thẻ hết hạn / đang bị khóa / có sách quá hạn / nợ phạt X đ / đã đủ hạn mức). Nút "Thu phạt ngay" / "Xem sách quá hạn" ngay tại đây. Admin có thể **ghi đè (override)** kèm lý do (log).
- Nếu độc giả có **đặt trước READY** → banner gợi ý "Độc giả có 1 sách đặt trước sẵn sàng" + nút thêm nhanh vào phiếu.
- Nếu có **yêu cầu online APPROVED** → nút "Nạp từ yêu cầu online" để chuyển thành phiếu giao (tránh tạo trùng).

Bước 2 — **Thêm sách vào phiếu**
- Ô **quét/nhập barcode bản sao** (Enter để thêm) hoặc tìm theo tên sách → chọn bản sao AVAILABLE cụ thể (hiển thị vị trí kệ).
- Mỗi lần thêm kiểm tra **ngay lập tức**: copy tồn tại? AVAILABLE? (nếu `HOLD` cho người khác → báo lỗi; nếu HOLD của chính độc giả → cho thêm) | `loanable`? (reference → báo "chỉ đọc tại chỗ") | trùng đầu sách (BR-06) | vượt hạn mức (BR-05) | trùng barcode trong phiếu.
- Bảng danh sách sách trong phiếu: STT | Ảnh | Tên sách | Barcode | Vị trí | Tình trạng (cho chỉnh `condition` nếu phát hiện) | **Ngày mượn** | **Hạn trả** (tự tính theo BR-09, cho phép staff chỉnh trong phạm vi cho phép; Admin chỉnh tự do) | Nút xóa.
- Hiển thị bộ đếm "Đã thêm 3/5".

Bước 3 — **Thông tin phiếu**
- Ngày mượn (mặc định hôm nay, không cho chọn quá khứ trừ Admin), ghi chú, (tùy chọn) tiền đặt cọc.
- Panel tóm tắt: số sách, hạn trả sớm nhất, cảnh báo.

Bước 4 — **Xác nhận**
- Nút `Xác nhận cho mượn` → **mở transaction DB**, kiểm tra lại toàn bộ điều kiện (chống race condition – khóa dòng copy `SELECT ... FOR UPDATE`), tạo `borrow_slip` + `borrow_slip_items`, copy → `BORROWED`, nếu copy đang HOLD của chính độc giả thì đóng reservation (`FULFILLED`), ghi audit log, gửi thông báo mượn thành công.
- Sau thành công: modal **In phiếu mượn** (khổ A5/80mm: logo, mã phiếu + QR, thông tin độc giả, bảng sách + hạn trả, quy định phạt, chữ ký) + nút `Tạo phiếu mới`.
- Nút phụ: `Lưu nháp` (DRAFT chỉ trong session/ localStorage hoặc bảng nháp), `Hủy`.

### 7.6 LUỒNG B — Mượn online (USER → STAFF)

**Phía USER (client)**
1. Tìm sách → trang chi tiết → nút **"Thêm vào giỏ mượn"** (chỉ hiện khi đăng nhập, còn bản AVAILABLE, loanable). Nếu hết bản → nút **"Đặt trước"**.
2. **Giỏ mượn** (`/borrow-cart`): danh sách sách đã chọn, hiển thị hạn mức còn lại, ngày dự kiến nhận (chọn ngày ghé thư viện trong `hold_hours`), ghi chú. Kiểm tra BR-01→BR-07, BR-12 ngay trên giỏ, báo lỗi từng dòng.
3. Bấm **Gửi yêu cầu mượn** → tạo slip `PENDING` (kênh ONLINE), thông báo "Yêu cầu đã gửi".
4. Theo dõi tại `/my/borrows`: trạng thái timeline (Đã gửi → Đã duyệt → Đã nhận sách → Đã trả). Có thể **hủy** khi PENDING/APPROVED.

**Phía STAFF (admin)**
1. Menu "Yêu cầu mượn online" (badge số lượng chờ). Bảng: mã, độc giả, số sách, ngày gửi, trạng thái, hạn mức hợp lệ?
2. Mở chi tiết → xem kiểm tra điều kiện lại tại thời điểm duyệt. Với **từng item**: gán **bản sao cụ thể** (hệ thống gợi ý copy AVAILABLE tốt nhất: tình trạng tốt, kệ gần) hoặc tự động gán.
3. `Duyệt` (có thể duyệt một phần: bỏ item không đủ điều kiện) → slip `APPROVED`, copy → `HOLD`, `pickup_deadline = now + hold_hours`, gửi thông báo cho USER ("Hãy đến nhận trước <thời gian>"). `Từ chối` → nhập lý do bắt buộc → `REJECTED`, thông báo.
4. Khi độc giả đến quầy: STAFF tìm phiếu (quét mã QR/mã phiếu/tên) → `Xác nhận giao sách` → quét barcode đối chiếu từng sách (đúng copy đã gán) → slip `BORROWING`, copy `BORROWED`, đặt `borrow_date = hôm nay`, `due_date` tính từ ngày giao thực tế (không tính từ ngày duyệt) → in phiếu.
5. Job nền (mỗi 15 phút): slip `APPROVED` quá `pickup_deadline` → `EXPIRED`, copy `HOLD → AVAILABLE` (hoặc cấp cho reservation kế tiếp), ghi nhận no-show (BR-14).

### 7.7 LUỒNG C — Trả sách (STAFF)

**Màn hình "Trả sách"**
1. Quét barcode sách (hoặc mã thẻ để xem toàn bộ sách đang mượn của độc giả, chọn nhiều cuốn trả một lúc).
2. Hệ thống tìm item đang `BORROWING` của copy → hiển thị: độc giả, phiếu, ngày mượn, hạn trả, **số ngày trễ**, **phạt trễ tự tính**, số lần đã gia hạn.
3. Staff chọn **Tình trạng khi trả**: `Bình thường` | `Hư hỏng nhẹ` | `Hư hỏng nặng` | `Mất` → nếu hỏng/mất: bắt buộc mô tả (+ ảnh) → tính phạt theo BR-30 (staff có thể chỉnh số tiền trong khoảng cho phép; vượt khoảng cần Admin).
4. Bảng tổng kết lượt trả: các sách, phạt từng sách, **tổng tiền phạt**.
5. Nút `Xác nhận trả`:
   - Item → `RETURNED` (hoặc `LOST`/`DAMAGED`), ghi `returned_at`, `returned_by`.
   - Copy → `AVAILABLE` (hoặc `DAMAGED/MAINTENANCE/LOST`).
   - Tạo `fines` (UNPAID) nếu có phạt; tùy chọn **Thu tiền ngay** (mở form thu phạt) hoặc ghi nợ.
   - **Kiểm tra hàng đợi đặt trước**: nếu đầu sách có reservation `WAITING` → copy chuyển `HOLD`, reservation `READY`, gửi thông báo, hiển thị **tờ nhắc "Cuốn này đã có người đặt – đặt vào kệ giữ chỗ"**.
   - Cập nhật trạng thái slip (`PARTIALLY_RETURNED`/`RETURNED`).
   - In biên nhận trả sách.
6. Trường hợp **trả sách không có thẻ/phiếu** (độc giả quên): vẫn xử lý được qua barcode copy.
7. Trường hợp **trả hộ**: cho phép, ghi chú người trả.

### 7.8 LUỒNG D — Gia hạn
- **Self-service (USER)**: tại `/my/borrows` → nút "Gia hạn" ở từng item đủ điều kiện (BR-16→18). Hiển thị hạn mới trước khi xác nhận. Nút bị vô hiệu + tooltip lý do khi không đủ điều kiện.
- **Staff**: gia hạn thay độc giả (có thể ghi đè giới hạn lần/ngày – chỉ ADMIN), ghi chú lý do.
- Mọi lần gia hạn ghi `renewals(item_id, old_due_date, new_due_date, performed_by, channel)`.

### 7.9 LUỒNG E — Mất sách / Hỏng sách ngoài lúc trả
- Độc giả báo mất → staff vào chi tiết item → `Báo mất sách` → hệ thống tạo phạt `LOST` (BR phạt), item `LOST`, copy `LOST`, phiếu cập nhật. Có thể **đền sách thay thế** (BR-29).

### 7.10 Danh sách phiếu mượn (màn hình quản lý)
- Bộ lọc: trạng thái, kênh, độc giả, khoảng ngày mượn, **chỉ quá hạn**, **sắp đến hạn (≤ 2 ngày)**, nhân viên lập, mã phiếu/barcode.
- Cột: Mã phiếu | Độc giả | Số sách | Ngày mượn | Hạn trả gần nhất | Trạng thái | Phạt | Nhân viên | Hành động (Xem, In, Trả, Gia hạn, Hủy).
- Tab nhanh: Tất cả | Chờ duyệt (online) | Chờ nhận sách | Đang mượn | Quá hạn | Đã trả | Đã hủy/hết hạn.
- Chi tiết phiếu: header + timeline sự kiện (tạo, duyệt, giao, gia hạn, trả, phạt) + bảng items + nút hành động theo trạng thái.
- **Quá hạn**: nút "Gửi nhắc" (email/in-app hàng loạt), xuất danh sách quá hạn Excel.

### 7.11 Validation tổng hợp khi tạo phiếu (checklist cho code)
```
validateBorrow(reader, copies[], context):
  ✔ reader.card.status == ACTIVE && card.expiry >= today               (BR-01)
  ✔ reader.user.status == ACTIVE && !reader.blacklisted                (BR-02)
  ✔ no item OVERDUE (nếu cấu hình bật)                                 (BR-03)
  ✔ unpaidFine(reader) <= settings.max_unpaid_fine                     (BR-04)
  ✔ currentBorrowing + pendingOnline + copies.length <= type.max_books (BR-05, BR-12)
  ✔ không trùng đầu sách (trong phiếu & đang mượn)                     (BR-06)
  ✔ mỗi copy: status AVAILABLE (hoặc HOLD của chính reader), loanable, !is_reference (BR-07, BR-08)
  ✔ tính due_date theo type/loan_days_override + né ngày nghỉ          (BR-09)
  ✔ thao tác trong transaction + khóa dòng copy chống mượn trùng đồng thời
```

---

## 8. THIẾT KẾ DATABASE

> Dùng UUID hoặc BIGINT auto-increment. Tất cả bảng có `created_at`, `updated_at`; bảng quan trọng có `deleted_at` (soft delete) và `created_by/updated_by`.

### 8.1 Tài khoản & phân quyền
**users**: id, email (unique), phone (unique), password_hash, full_name, avatar_url, role (`ADMIN|STAFF|USER`), status (`PENDING|ACTIVE|LOCKED|INACTIVE`), email_verified_at, last_login_at, failed_login_count, locked_until, created_at, updated_at.

**staff_profiles**: id, user_id (FK), staff_code (unique), position, hire_date, shift, note.

**refresh_tokens**: id, user_id, token_hash, expires_at, revoked_at, user_agent, ip.

**password_resets**: id, user_id, token_hash, expires_at, used_at.

**permissions** *(nâng cao)*: id, code, name. **role_permissions**: role, permission_id.

### 8.2 Độc giả & thẻ
**reader_types**: id, code, name, max_books, loan_days, max_renewals, renew_days, max_reservations, card_fee, card_validity_months, is_active.

**readers**: id, user_id (FK unique), reader_code, reader_type_id, dob, gender, address, id_number (CCCD), student_code, organization (lớp/khoa/đơn vị), emergency_contact, note, blacklisted (bool), blacklist_reason, no_show_count, registered_at.

**library_cards**: id, reader_id, card_number (unique), barcode, issued_at, expires_at, status (`PENDING|ACTIVE|EXPIRED|SUSPENDED|LOST|CANCELLED`), fee_paid, replaced_by_card_id, suspended_reason, issued_by.

### 8.3 Danh mục & sách
**categories**: id, parent_id, code, name, description, sort_order, is_active.
**authors**: id, name, pen_name, nationality, birth_year, death_year, bio, photo_url.
**publishers**: id, name, address, phone, email, website.
**languages**: id, code, name. **document_types**: id, code, name.
**locations** *(shelves)*: id, code (unique), zone, floor, row, shelf, slot, capacity, note.

**books** (đầu sách): id, isbn13, isbn10, title, original_title, subtitle, publisher_id, publish_year, edition, language_id, document_type_id, pages, size, price, description, cover_url, classification_code, tags (jsonb/text[]), is_reference, loanable, loan_days_override, status (`ACTIVE|HIDDEN`), avg_rating, rating_count, borrow_count, search_vector (tsvector).
**book_authors**: book_id, author_id, role (`AUTHOR|TRANSLATOR|EDITOR`), sort_order.
**book_categories**: book_id, category_id.

**book_copies**: id, book_id, barcode (unique), copy_no, location_id, status (`AVAILABLE|BORROWED|HOLD|LOST|DAMAGED|MAINTENANCE|WITHDRAWN|IN_TRANSIT`), condition (`NEW|GOOD|FAIR|POOR`), purchase_price, acquired_at, acquisition_source (`PURCHASE|DONATION|TRANSFER|REPLACEMENT`), import_item_id, note, withdrawn_at, withdrawn_reason.
**copy_history**: id, copy_id, event, from_status, to_status, ref_type, ref_id, actor_id, note, created_at.

### 8.4 Mượn – trả
**borrow_slips**: id, slip_code (unique), reader_id, channel (`COUNTER|ONLINE`), status (`PENDING|APPROVED|BORROWING|PARTIALLY_RETURNED|RETURNED|REJECTED|CANCELLED|EXPIRED`), requested_at, approved_at, approved_by, rejected_reason, pickup_deadline, borrowed_at (ngày giao), issued_by (nhân viên giao), deposit_amount, note, cancelled_at, cancelled_by, cancel_reason.

**borrow_slip_items**: id, slip_id, book_id, copy_id (nullable khi chưa gán), status (`REQUESTED|HOLD|BORROWING|RETURNED|LOST|DAMAGED|CANCELLED|EXPIRED`), borrowed_at, due_date, returned_at, returned_by, renew_count, condition_out, condition_in, return_note, damage_images (jsonb).

**renewals**: id, item_id, old_due_date, new_due_date, channel (`ONLINE|COUNTER`), performed_by, note, created_at.

**reservations**: id, reader_id, book_id, status (`WAITING|READY|FULFILLED|CANCELLED|EXPIRED`), queue_position, assigned_copy_id, ready_at, ready_until, fulfilled_slip_id, created_at.

### 8.5 Phạt & thanh toán
**fines**: id, reader_id, slip_item_id (nullable), type (`OVERDUE|DAMAGED_MINOR|DAMAGED_MAJOR|LOST|CARD_REISSUE|OTHER`), amount, paid_amount, status (`UNPAID|PARTIAL|PAID|WAIVED|REDUCED`), overdue_days, reason, waived_by, waived_reason, created_by, created_at.
**payments**: id, receipt_code (unique), reader_id, amount, method (`CASH|TRANSFER|WALLET|ONLINE`), status (`COMPLETED|PENDING_CONFIRM|REFUNDED`), received_by, paid_at, note.
**payment_allocations**: payment_id, fine_id, amount.

### 8.6 Kho
**suppliers**: id, name, contact_name, phone, email, address, tax_code, note, is_active.
**import_receipts**: id, receipt_code, supplier_id, source, status (`DRAFT|PENDING_APPROVAL|COMPLETED|CANCELLED`), total_amount, note, created_by, approved_by, completed_at.
**import_items**: id, receipt_id, book_id, quantity, unit_price, location_id.
**inventory_audits**: id, code, scope_type, scope_value, status (`OPEN|CLOSED`), started_at, closed_at, created_by.
**inventory_audit_items**: id, audit_id, copy_id, expected_location_id, found (bool), found_at, scanned_by, result (`MATCH|MISSING|EXTRA|WRONG_LOCATION`), note.
**disposal_receipts / disposal_items**: lý do, copy_id, duyệt bởi.

### 8.7 Khác
**notifications**: id, user_id, type, title, body, data (jsonb), channel, is_read, read_at, sent_at, created_at.
**notification_templates**: code, subject, body, channel, is_active.
**settings**: key (unique), value (jsonb), description, updated_by.
**holidays**: id, date, name, is_recurring.
**favorites**: user_id, book_id. **reviews**: id, user_id, book_id, rating, comment, status (`VISIBLE|HIDDEN`).
**news**: id, title, slug, cover_url, content, status, is_pinned, published_at, author_id. **banners**: id, image_url, link, sort_order, is_active.
**purchase_suggestions**: id, user_id, title, author, isbn, reason, status (`NEW|ACCEPTED|REJECTED`).
**audit_logs**: id, actor_id, action, entity_type, entity_id, before (jsonb), after (jsonb), ip, user_agent, created_at.

### 8.8 Index & ràng buộc quan trọng
- Unique: `users.email`, `library_cards.card_number`, `book_copies.barcode`, `borrow_slips.slip_code`.
- Index: `book_copies(book_id,status)`, `borrow_slip_items(copy_id,status)`, `borrow_slip_items(due_date,status)`, `borrow_slips(reader_id,status)`, `reservations(book_id,status,created_at)`, `fines(reader_id,status)`, GIN trên `books.search_vector`.
- **Partial unique**: 1 copy chỉ có tối đa **1 item đang BORROWING/HOLD**: `UNIQUE(copy_id) WHERE status IN ('HOLD','BORROWING')`.
- Reader không có 2 item BORROWING cùng `book_id`.
- Check constraint: `due_date >= borrowed_at`, `paid_amount <= amount`.


---

## 9. THIẾT KẾ API (REST, prefix `/api/v1`)

Quy ước: response `{ success, data, message, meta:{page,limit,total} }`; lỗi `{ success:false, code, message, errors[] }`; phân trang `?page=&limit=&sort=&q=`; mã lỗi nghiệp vụ dạng `BORROW_LIMIT_EXCEEDED`, `READER_HAS_OVERDUE`, `COPY_NOT_AVAILABLE`, `CARD_EXPIRED`, `RENEW_NOT_ALLOWED`…

### 9.1 Auth
| Method | Endpoint | Quyền |
|---|---|---|
| POST | /auth/register | Public |
| POST | /auth/login | Public |
| POST | /auth/refresh, /auth/logout | Auth |
| POST | /auth/forgot-password, /auth/reset-password | Public |
| POST | /auth/change-password | Auth |
| GET | /auth/me | Auth |
| POST | /auth/verify-email | Public |

### 9.2 Nhân viên / Độc giả / Thẻ
| Method | Endpoint | Quyền |
|---|---|---|
| CRUD | /staff, PATCH /staff/:id/status | ADMIN |
| GET/POST | /readers | STAFF+ |
| GET/PUT | /readers/:id | STAFF+ (USER: /me) |
| GET | /readers/:id/eligibility | STAFF+ → kết quả kiểm tra BR-01→05 |
| GET | /readers/:id/borrowing, /history, /fines, /reservations | STAFF+ |
| PATCH | /readers/:id/block, /unblock, /blacklist | STAFF+/ADMIN |
| POST | /readers/import | STAFF+ |
| GET | /readers/lookup?card= | STAFF+ (quét thẻ) |
| POST | /cards, PATCH /cards/:id/renew, /cards/:id/report-lost | STAFF+ |
| CRUD | /reader-types | ADMIN |

### 9.3 Danh mục & sách
| Method | Endpoint | Quyền |
|---|---|---|
| CRUD | /categories, /authors, /publishers, /locations, /languages | GET public; ghi STAFF+ |
| GET | /books?q=&category=&author=&publisher=&year=&language=&available=&sort= | Public |
| GET | /books/:id (kèm copies tóm tắt, rating) | Public |
| POST/PUT/DELETE | /books | STAFF+ (DELETE ADMIN) |
| POST | /books/import, GET /books/export | STAFF+ |
| POST | /books/:id/cover | STAFF+ |
| GET | /books/:id/related, /books/new, /books/popular | Public |
| GET | /books/:id/copies | STAFF+ |
| POST | /books/:id/copies (số lượng → sinh barcode) | STAFF+ |
| PATCH | /copies/:id (đổi trạng thái/vị trí/tình trạng), GET /copies/:id/history | STAFF+ |
| GET | /copies/lookup?barcode= | STAFF+ |
| POST | /copies/labels (PDF tem) | STAFF+ |

### 9.4 Mượn – trả (QUAN TRỌNG)
| Method | Endpoint | Mô tả | Quyền |
|---|---|---|---|
| POST | /borrows/counter | Tạo phiếu tại quầy `{readerId, items:[{copyId, dueDate?}], note, deposit}` | STAFF+ |
| POST | /borrows/validate | Kiểm tra trước (dry-run) | STAFF+ / USER |
| POST | /borrows/online | USER gửi yêu cầu `{items:[{bookId}], pickupDate, note}` | USER |
| GET | /borrows | Danh sách + lọc | STAFF+ |
| GET | /borrows/me | Phiếu của tôi | USER |
| GET | /borrows/:id | Chi tiết + timeline | STAFF+/chủ phiếu |
| POST | /borrows/:id/approve | `{items:[{itemId, copyId?}]}` | STAFF+ |
| POST | /borrows/:id/reject | `{reason}` | STAFF+ |
| POST | /borrows/:id/cancel | | USER(chủ)/STAFF+ |
| POST | /borrows/:id/handover | Giao sách (xác nhận lấy) `{scannedBarcodes[]}` | STAFF+ |
| POST | /returns | Trả sách `{items:[{copyBarcode, condition, note, images, fineOverride?}], collectPayment?}` | STAFF+ |
| GET | /returns/preview?barcode= | Xem phạt dự kiến trước khi xác nhận | STAFF+ |
| POST | /borrow-items/:id/renew | Gia hạn (USER chủ / STAFF+) | USER/STAFF+ |
| GET | /borrow-items/:id/renew-eligibility | | USER/STAFF+ |
| POST | /borrow-items/:id/report-lost | | STAFF+ |
| GET | /borrows/overdue, /borrows/due-soon | Báo động | STAFF+ |
| POST | /borrows/overdue/remind | Gửi nhắc hàng loạt | STAFF+ |
| GET | /borrows/:id/print | PDF phiếu | STAFF+/chủ phiếu |

### 9.5 Đặt trước, Phạt, Thanh toán
| Method | Endpoint | Quyền |
|---|---|---|
| POST | /reservations `{bookId}` | USER/STAFF+ |
| GET | /reservations, /reservations/me | STAFF+ / USER |
| POST | /reservations/:id/cancel | USER(chủ)/STAFF+ |
| GET | /fines, /fines/me | STAFF+ / USER |
| POST | /fines (phạt thủ công) | STAFF+ |
| POST | /fines/:id/waive `{reason, amount?}` | ADMIN |
| POST | /payments `{readerId, fineIds[], amount, method}` | STAFF+ |
| POST | /payments/request (USER yêu cầu nộp) | USER |
| POST | /payments/:id/confirm, /payments/:id/refund | STAFF+/ADMIN |
| GET | /payments/:id/receipt (PDF) | STAFF+ |

### 9.6 Kho & báo cáo & khác
| Endpoint | Quyền |
|---|---|
| CRUD /suppliers, /imports (+ /imports/:id/submit, /approve, /complete) | STAFF+/ADMIN |
| CRUD /inventory-audits (+ /scan, /close, /apply-adjustments) | STAFF+/ADMIN |
| CRUD /disposals (+ /approve) | ADMIN |
| GET /notifications/me, PATCH /notifications/:id/read, /read-all | Auth |
| CRUD /notification-templates | ADMIN |
| GET /reports/dashboard, /reports/borrow-stats, /reports/top-books, /reports/top-readers, /reports/overdue, /reports/fines, /reports/inventory, /reports/new-readers (+ `?export=xlsx`) | STAFF+ (một số ADMIN) |
| GET/PUT /settings | STAFF xem, ADMIN sửa |
| CRUD /holidays | ADMIN |
| GET /audit-logs | ADMIN |
| CRUD /news, /banners; GET public | STAFF+ |
| POST/GET /reviews, /favorites | USER |
| POST /purchase-suggestions | USER |

### 9.7 Job nền (cron)
| Job | Tần suất | Việc |
|---|---|---|
| expireUnclaimedSlips | 15 phút | APPROVED quá `pickup_deadline` → EXPIRED, nhả copy, cấp cho reservation kế |
| expireReadyReservations | 15 phút | READY quá `ready_until` → EXPIRED, chuyển người kế |
| dueSoonReminder | hằng ngày 08:00 | Nhắc sách sắp đến hạn (T-2) |
| overdueReminder | hằng ngày 08:00 | Nhắc quá hạn định kỳ |
| cardExpiryReminder | hằng ngày | Thẻ sắp hết hạn T-30/T-7; tự chuyển `EXPIRED` |
| accrueOverdueFines (tùy chọn) | hằng đêm | Cập nhật phạt dự kiến |
| backupDb | hằng đêm | Sao lưu |

---

## 10. GIAO DIỆN ADMIN (admin-web) — TỪNG TRANG

### 10.1 Layout chung
- **Sidebar trái** (thu gọn được) theo nhóm, ẩn mục theo role; **Topbar**: ô tìm kiếm nhanh toàn cục (Ctrl+K: tìm sách/độc giả/phiếu), chuông thông báo, avatar menu (Hồ sơ, Đổi mật khẩu, Đăng xuất), chuyển Dark/Light.
- Breadcrumb, bảng dữ liệu chuẩn (sắp xếp, lọc, phân trang, chọn cột, export Excel), toast, modal xác nhận, skeleton loading, empty state, responsive tablet trở lên.
- Màu trạng thái chuẩn: Xanh = tốt/available/paid, Vàng = chờ/sắp hạn, Đỏ = quá hạn/mất/unpaid, Xám = đã đóng, Xanh dương = đang mượn.

**Menu:**
```
🏠 Dashboard
📖 Mượn – Trả
   ├─ Tạo phiếu mượn (quầy)        [STAFF+]
   ├─ Trả sách                      [STAFF+]
   ├─ Yêu cầu mượn online  (badge)  [STAFF+]
   ├─ Danh sách phiếu mượn          [STAFF+]
   ├─ Sách quá hạn                  [STAFF+]
   ├─ Đặt trước                     [STAFF+]
   └─ Gia hạn (lịch sử)             [STAFF+]
💰 Phạt & Thu tiền
   ├─ Khoản phạt
   └─ Biên lai thanh toán
👥 Độc giả
   ├─ Danh sách độc giả
   ├─ Thẻ thư viện
   ├─ Duyệt đăng ký mới (badge)
   └─ Loại độc giả                  [ADMIN]
📚 Kho sách
   ├─ Đầu sách
   ├─ Bản sao & Barcode
   ├─ Thể loại / Tác giả / NXB / Vị trí kệ
   ├─ Nhập sách (Phiếu nhập) / Nhà cung cấp
   ├─ Kiểm kê
   └─ Thanh lý                      [ADMIN]
📰 Nội dung: Tin tức, Banner, Đánh giá, Đề xuất mua sách
📊 Báo cáo & Thống kê
⚙️ Hệ thống [ADMIN]: Nhân viên, Phân quyền, Cấu hình, Ngày nghỉ, Mẫu thông báo, Nhật ký hoạt động
```

### 10.2 Dashboard
- **Thẻ KPI** (hôm nay/tháng): Tổng đầu sách, Tổng bản sao, Đang mượn, Quá hạn (đỏ, bấm vào → danh sách), Phiếu mượn hôm nay, Lượt trả hôm nay, Yêu cầu online chờ duyệt, Độc giả mới, Tiền phạt thu hôm nay, Phạt chưa thu.
- **Biểu đồ**: lượt mượn/trả 30 ngày (line), mượn theo thể loại (pie/donut), top 10 sách mượn nhiều (bar), phân bố độc giả theo loại, doanh thu phạt theo tháng.
- **Danh sách cần xử lý**: yêu cầu chờ duyệt, sách đặt trước sẵn sàng sắp hết hạn giữ, sách sắp đến hạn hôm nay/ngày mai, thẻ sắp hết hạn, tồn kho thấp.
- Staff thấy dashboard vận hành; Admin thêm tài chính & hệ thống.

### 10.3 Trang "Tạo phiếu mượn" — xem chi tiết mục 7.5
Layout 2 cột: **Trái** (60%): ô quét barcode + bảng sách trong phiếu; **Phải** (40%): thẻ độc giả + trạng thái điều kiện + tóm tắt phiếu + nút Xác nhận. Phím tắt: `F2` focus ô độc giả, `F3` focus barcode sách, `Ctrl+Enter` xác nhận, `Esc` hủy. Âm thanh beep khi quét thành công/lỗi.

### 10.4 Trang "Trả sách" — xem mục 7.7
Ô quét lớn trên cùng; bảng "Sách đang trả" (có phạt từng dòng, chọn tình trạng bằng dropdown); khung tổng phạt; nút `Xác nhận trả` & `Thu tiền ngay`; panel bên phải "Thông tin độc giả & các sách còn đang mượn".

### 10.5 Trang "Yêu cầu mượn online"
Bảng + tab (Chờ duyệt / Đã duyệt-chờ nhận / Hết hạn / Từ chối). Chi tiết dạng drawer/modal: độc giả, kết quả kiểm tra điều kiện, danh sách sách với dropdown chọn bản sao, nút Duyệt / Duyệt một phần / Từ chối (lý do). Tab "Chờ nhận": ô tìm mã phiếu + nút Xác nhận giao (quét barcode).

### 10.6 Trang "Danh sách phiếu mượn" & "Chi tiết phiếu"
Xem 7.10. Chi tiết phiếu có: header, timeline, bảng items (mỗi dòng: Trả / Gia hạn / Báo mất), tab Phạt liên quan, nút In phiếu, Hủy phiếu (chỉ khi chưa giao).

### 10.7 Trang "Sách quá hạn"
Bảng: Độc giả | SĐT | Sách | Hạn trả | Số ngày trễ | Phạt dự kiến | Lần nhắc gần nhất | Hành động (Gọi/Gửi nhắc/Trả sách/Báo mất). Chọn nhiều → "Gửi nhắc hàng loạt". Lọc theo số ngày trễ (1–7, 8–30, >30). Export.

### 10.8 Trang "Đặt trước"
Bảng theo đầu sách: người đặt, vị trí hàng đợi, trạng thái, thời hạn giữ (đếm ngược). Hành động: Hủy, Chuyển người kế, Gia hạn giữ (Admin), Giao sách (chuyển thành phiếu mượn có sẵn sách).

### 10.9 Trang "Độc giả"
- **List**: ảnh, mã thẻ, tên, loại, SĐT, hạn thẻ, đang mượn/tối đa, phạt nợ, badge trạng thái. Nút `+ Thêm độc giả`, `Import Excel`.
- **Form thêm/sửa** (stepper): Thông tin cá nhân → Loại & thẻ (chọn loại, thời hạn, thu phí làm thẻ) → Tài khoản đăng nhập (tự sinh mật khẩu tạm, gửi email) → Xác nhận & in thẻ.
- **Chi tiết** dạng tab như M3; thanh hành động: Tạo phiếu mượn, Khóa/Mở, Gia hạn thẻ, Báo mất thẻ, Thu phạt, Blacklist.
- **Duyệt đăng ký mới**: danh sách PENDING, xem thông tin/giấy tờ, Duyệt (tạo thẻ) / Từ chối (lý do).
- **Thẻ thư viện**: danh sách thẻ, in thẻ (mẫu CR80, có ảnh + barcode + QR), lịch sử cấp.

### 10.10 Trang "Kho sách"
- **Đầu sách – list**: ảnh bìa, tên, tác giả, ISBN, thể loại, NXB, tổng/có sẵn, trạng thái; lọc đa tiêu chí; chế độ Grid/List; thao tác hàng loạt (ẩn/hiện, xuất, in tem).
- **Form đầu sách**: chia section (Thông tin cơ bản, Phân loại, Xuất bản, Mô tả & ảnh, Cấu hình mượn, Bản sao ban đầu: số lượng/vị trí/giá nhập). Tạo nhanh tác giả/NXB/thể loại inline (modal).
- **Chi tiết đầu sách**: thông tin + tab Bản sao (bảng barcode, vị trí, trạng thái, tình trạng, hành động, người đang mượn) + tab Lịch sử mượn + tab Đặt trước + tab Đánh giá.
- **Bản sao**: bảng toàn cục, tìm theo barcode, lọc trạng thái/vị trí/tình trạng, đổi trạng thái hàng loạt, in tem.
- **Danh mục** (Thể loại dạng cây kéo thả, Tác giả, NXB, Vị trí kệ): CRUD dạng bảng + modal, có gộp trùng.
- **Phiếu nhập**: list + form (chọn NCC, thêm dòng sách/số lượng/giá/kệ, tổng tiền) + quy trình duyệt + khi hoàn tất hiển thị link in tem.
- **Kiểm kê**: tạo đợt → màn hình quét (hiển thị tiến độ x/y, danh sách đã/chưa quét, cảnh báo sai vị trí) → kết quả đối chiếu → duyệt điều chỉnh.

### 10.11 Trang "Phạt & Thu tiền"
- Bảng khoản phạt: mã, độc giả, loại, số tiền, đã trả, còn nợ, trạng thái, ngày tạo, phiếu liên quan. Lọc, tổng hợp (tổng phát sinh / đã thu / còn nợ).
- Form thu tiền: chọn độc giả → tick các khoản → nhập số tiền → phương thức → in biên lai.
- Miễn/giảm (Admin): modal lý do bắt buộc.
- Biên lai: danh sách, xem/in lại, hoàn tiền.

### 10.12 Trang "Báo cáo"
Bộ lọc thời gian (hôm nay/tuần/tháng/quý/năm/tùy chọn) + so sánh kỳ trước. Báo cáo:
1. Thống kê mượn–trả (theo ngày/tháng, theo thể loại, theo loại độc giả).
2. Top sách mượn nhiều/ít; sách chưa từng được mượn.
3. Top độc giả tích cực; độc giả vi phạm.
4. Quá hạn (theo mức, tỷ lệ quá hạn).
5. Doanh thu phạt/phí thẻ; công nợ phạt.
6. Tồn kho (theo thể loại/trạng thái, mất/hỏng/thanh lý).
7. Nhập sách theo thời gian/NCC.
8. Hiệu suất nhân viên (số phiếu lập/nhận trả).
9. Độc giả mới & thẻ hết hạn.
Mỗi báo cáo: biểu đồ + bảng + Export Excel/PDF.

### 10.13 Trang "Hệ thống" [ADMIN]
- **Nhân viên**: CRUD, đổi vai trò, khóa, reset mật khẩu, xem log.
- **Cấu hình**: form theo nhóm (Thông tin thư viện, Chính sách mượn, Phạt, Thông báo, Giờ làm việc) + CRUD Loại độc giả + Ngày nghỉ/lễ (lịch).
- **Mẫu thông báo**: sửa nội dung email/in-app kèm xem trước.
- **Nhật ký**: bảng log (thời gian, người, hành động, đối tượng, IP) + xem diff before/after + lọc + export.

### 10.14 Trang phụ
Đăng nhập admin, Quên mật khẩu, Hồ sơ cá nhân/Đổi mật khẩu, 403, 404, Trang in (phiếu mượn, biên lai, thẻ, tem barcode) dùng CSS `@media print`.

---

## 11. GIAO DIỆN CLIENT (client-web) — TỪNG TRANG

### 11.1 Layout chung
Header: logo, ô tìm kiếm lớn (gợi ý autocomplete theo tên/tác giả), menu (Trang chủ | Danh mục | Sách mới | Tin tức | Giới thiệu & Nội quy | Liên hệ), giỏ mượn (badge số lượng), chuông thông báo, avatar/Đăng nhập. Footer: thông tin thư viện, giờ mở cửa, link nhanh. Responsive **mobile-first**, hỗ trợ dark mode.

### 11.2 Danh sách trang
| Route | Trang | Quyền | Nội dung chính |
|---|---|---|---|
| `/` | Trang chủ | Public | Banner slider, ô tìm kiếm nổi bật, sách mới, sách mượn nhiều, thể loại nổi bật (icon), tin tức, thống kê thư viện, thông báo nghỉ lễ |
| `/books` | Tra cứu sách | Public | Sidebar lọc (thể loại cây, tác giả, NXB, năm, ngôn ngữ, **chỉ còn sách**, đánh giá), sắp xếp (liên quan/mới/mượn nhiều/A-Z), grid/list, phân trang hoặc infinite scroll, highlight từ khóa |
| `/books/:id` | Chi tiết sách | Public | Ảnh bìa, tên, tác giả (link), NXB, năm, ISBN, thể loại, số trang, mô tả (xem thêm), vị trí kệ, **tình trạng tồn (Còn x/y bản)**, rating; nút **Thêm vào giỏ mượn** / **Đặt trước** (nếu hết) / ♡ Yêu thích; tab Đánh giá; sách liên quan; "Cùng tác giả" |
| `/authors/:id`, `/categories/:slug` | Trang tác giả/thể loại | Public | Thông tin + danh sách sách |
| `/news`, `/news/:slug` | Tin tức | Public | List + chi tiết |
| `/about`, `/rules`, `/contact` | Giới thiệu, Nội quy (nêu rõ quy định mượn/phạt), Liên hệ (bản đồ, giờ mở cửa) | Public | Trang tĩnh quản trị từ admin |
| `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` | Xác thực | Guest | Form validate Zod, thông báo rõ ràng |
| `/borrow-cart` | **Giỏ mượn** | USER | Xem mục 7.6, kiểm tra điều kiện từng dòng, chọn ngày nhận, gửi yêu cầu |
| `/my/dashboard` | Tổng quan tài khoản | USER | Thẻ thư viện (QR/barcode, hạn thẻ), số đang mượn/tối đa, sắp đến hạn, quá hạn, phạt chưa nộp, thông báo mới |
| `/my/borrows` | **Sách đang mượn & Yêu cầu mượn** | USER | Tab: Yêu cầu (PENDING/APPROVED – đếm ngược hạn nhận, nút Hủy) | Đang mượn (hạn trả, badge sắp hạn/quá hạn, nút **Gia hạn**, phạt dự kiến) |
| `/my/history` | Lịch sử mượn trả | USER | Bảng/timeline, lọc thời gian/trạng thái, xem lại phiếu, nút "Mượn lại", xuất |
| `/my/borrows/:id` | Chi tiết phiếu | USER | Timeline, danh sách sách, mã QR phiếu để xuất trình tại quầy |
| `/my/reservations` | Sách đặt trước | USER | Vị trí hàng đợi, trạng thái, hạn nhận (READY), nút Hủy |
| `/my/fines` | Phạt & thanh toán | USER | Danh sách khoản phạt, tổng nợ, lịch sử biên lai, nút "Gửi yêu cầu thanh toán", hướng dẫn nộp tại quầy/chuyển khoản |
| `/my/favorites` | Sách yêu thích | USER | Grid, bỏ yêu thích, thêm giỏ |
| `/my/reviews` | Đánh giá của tôi | USER | Sửa/xóa |
| `/my/notifications` | Thông báo | USER | List, lọc đã/chưa đọc, đánh dấu đã đọc |
| `/my/profile` | Hồ sơ | USER | Sửa thông tin, ảnh, đổi mật khẩu, thông tin thẻ, **yêu cầu gia hạn thẻ/báo mất thẻ**, cài đặt nhận thông báo |
| `/suggest` | Đề xuất mua sách | USER | Form gợi ý sách thư viện chưa có |

### 11.3 Hành vi UX quan trọng (client)
- Nút "Thêm vào giỏ mượn" kiểm tra trạng thái đăng nhập → chuyển login và quay lại.
- Trên trang chi tiết hiển thị **badge**: 🟢 Còn sách | 🟡 Còn ít (≤ 1) | 🔴 Hết – có thể đặt trước (kèm số người đang chờ, ngày dự kiến có sách = hạn trả sớm nhất) | ⚪ Chỉ đọc tại chỗ.
- Cảnh báo nổi bật trong layout khi USER **quá hạn/nợ phạt/thẻ sắp hết hạn** (banner đỏ/vàng).
- Giỏ mượn lưu server-side (để đồng bộ nhiều thiết bị) hoặc localStorage khi chưa gửi.
- Trạng thái rỗng thân thiện + CTA; skeleton; xử lý lỗi mạng.
- SEO cơ bản cho trang sách (meta, Open Graph) nếu dùng Next.js SSR.

---

## 12. THÔNG BÁO, BÁO CÁO, NHẬT KÝ

### 12.1 Audit log — ghi bắt buộc các hành động
Đăng nhập (thành công/thất bại), tạo/sửa/xóa sách/copy, tạo phiếu mượn, duyệt/từ chối/hủy, giao sách, trả sách, gia hạn, tạo/miễn/thu phạt, đổi trạng thái copy, khóa/mở khóa độc giả, đổi cấu hình, đổi quyền, thanh lý, điều chỉnh kiểm kê, override điều kiện mượn. Lưu `before/after`, actor, IP.

### 12.2 In ấn
Phiếu mượn (A5 & 80mm), phiếu trả/biên nhận, biên lai thu tiền, thẻ thư viện (CR80), tem barcode copy (lưới A4), biên bản kiểm kê, danh sách quá hạn. Tất cả dạng trang in HTML/PDF có logo & thông tin thư viện từ settings.

---

## 13. PHI CHỨC NĂNG

- **Bảo mật**: bcrypt, JWT httpOnly cookie hoặc header, CORS whitelist, rate limit (login, API), helmet, validate input (Zod/class-validator), chống SQL injection (ORM), XSS (sanitize rich text), CSRF nếu dùng cookie, upload kiểm tra mime/size, ẩn thông tin nhạy cảm (CCCD che giữa) trên UI trừ khi có quyền.
- **Toàn vẹn dữ liệu**: mọi nghiệp vụ mượn/trả/duyệt/thu tiền chạy trong **transaction**; khóa dòng chống race; idempotency key cho thao tác xác nhận.
- **Hiệu năng**: phân trang server-side, index như 8.8, cache danh mục/settings, trang tìm kiếm < 500ms, lazy load ảnh, quét barcode phản hồi < 300ms.
- **Khả dụng**: phím tắt & thao tác bằng máy quét cho quầy; giao diện admin tối ưu màn hình 1366px+; client responsive.
- **Đa ngôn ngữ** (tùy chọn): i18n vi/en, mặc định **tiếng Việt**. Định dạng ngày `DD/MM/YYYY`, tiền `1.000đ`, múi giờ `Asia/Ho_Chi_Minh`.
- **Kiểm thử**: unit test cho service nghiệp vụ (tính phạt, tính hạn, validate), e2e các luồng chính, seed data.
- **Triển khai**: Docker Compose (api, db, admin, client, redis), `.env.example`, migration, README hướng dẫn chạy.
- **Tiện ích**: soft delete, export Excel, backup, log lỗi (Winston/Pino), healthcheck.

---

## 14. DỮ LIỆU MẪU & TEST CASE

### 14.1 Seed
- 1 Admin (`admin@lib.vn`), 2 Staff, 20 độc giả (đủ 5 loại, có người hết hạn thẻ, bị khóa, nợ phạt, có sách quá hạn), 15 thể loại (cây), 30 tác giả, 10 NXB, 12 kệ, **200 đầu sách / 500 bản sao** (ảnh bìa placeholder), vài phiếu mượn ở đủ mọi trạng thái, vài reservation, fines, 5 tin tức, 3 banner, ngày lễ VN (Tết, 30/4, 1/5, 2/9, Giỗ Tổ), settings mặc định, mẫu thông báo.

### 14.2 Test case nghiệp vụ tối thiểu
1. Mượn khi thẻ hết hạn → từ chối `CARD_EXPIRED`.
2. Mượn khi có sách quá hạn → từ chối `READER_HAS_OVERDUE`.
3. Mượn vượt hạn mức → `BORROW_LIMIT_EXCEEDED` (tính cả yêu cầu online đang chờ).
4. Mượn 2 bản cùng đầu sách → từ chối.
5. Mượn sách reference → từ chối.
6. Hai staff cùng quét 1 copy đồng thời → chỉ 1 thành công.
7. `due_date` rơi Chủ nhật/ngày lễ → dời sang ngày làm việc.
8. Trả trễ 5 ngày × 2.000đ = 10.000đ; trễ dài → phạt không vượt giá bìa.
9. Trả sách có người đặt trước → copy thành HOLD, reservation READY, gửi thông báo.
10. Gia hạn khi có người đặt trước → từ chối; gia hạn quá số lần → từ chối; gia hạn hợp lệ → hạn mới = hạn cũ + N ngày.
11. Online: APPROVED quá hạn nhận → EXPIRED, copy nhả, tăng no-show.
12. Báo mất → tạo phạt LOST = giá × 2, copy `LOST`.
13. Thu phạt một phần → status `PARTIAL`; thu đủ → `PAID`; miễn → `WAIVED` có log.
14. Staff không thể miễn phạt/sửa cấu hình (403); USER không xem được dữ liệu người khác (403).
15. Kiểm kê: copy không quét → MISSING → sau duyệt chuyển LOST.

---

## 15. LỘ TRÌNH TRIỂN KHAI CHO CURSOR (PROMPT THEO PHASE)

> Làm tuần tự, mỗi phase chạy được và test được trước khi sang phase sau. Luôn: TypeScript strict, tách layer (controller → service → repository), validate bằng Zod/DTO, dùng enum từ `packages/shared`, **không hard-code chính sách** (đọc từ settings/reader_types).

**Phase 0 – Khởi tạo**: monorepo, cấu hình lint/prettier, Docker Compose (Postgres), Prisma schema đầy đủ theo mục 8, migration, seed (mục 14), shared package (enums, types, zod).

**Phase 1 – Auth & RBAC & Nền tảng UI**: module auth (register/login/refresh/reset), guard roles, middleware audit; admin-web & client-web dựng layout, routing, route guard, axios + interceptor refresh token, theme, component bảng/form/modal dùng chung.

**Phase 2 – Danh mục & Sách & Bản sao**: CRUD category/author/publisher/location; books + upload bìa; sinh copies + barcode; tìm kiếm full-text; trang admin kho sách + trang client tra cứu & chi tiết; in tem barcode.

**Phase 3 – Độc giả & Thẻ**: reader_types, readers, cards; trang admin độc giả (list/form/chi tiết/duyệt đăng ký/in thẻ); client đăng ký + hồ sơ; API `eligibility`.

**Phase 4 – ⭐ Mượn/Trả (lõi)**: service `BorrowPolicyService` (validate BR-01→12, tính due date, holidays), `FineService` (tính phạt), API mục 9.4; admin: **Tạo phiếu mượn (quầy)**, **Trả sách**, danh sách + chi tiết phiếu, in phiếu; unit test cho policy/fine.

**Phase 5 – Mượn online, Đặt trước, Gia hạn**: giỏ mượn client, yêu cầu mượn, duyệt/giao ở admin, reservations + hàng đợi, gia hạn self-service, các cron job (9.7).

**Phase 6 – Phạt & Thanh toán**: fines/payments/biên lai, miễn phạt (Admin), client xem phạt & gửi yêu cầu nộp.

**Phase 7 – Thông báo & Nội dung**: notification service (in-app + email, template), chuông thông báo cả 2 app, tin tức/banner, đánh giá/yêu thích, đề xuất mua sách.

**Phase 8 – Kho nâng cao**: nhà cung cấp, phiếu nhập (quy trình duyệt), kiểm kê, thanh lý, mất/hỏng.

**Phase 9 – Báo cáo & Hệ thống**: dashboard, các báo cáo + export, quản lý nhân viên, cấu hình, audit log viewer.

**Phase 10 – Hoàn thiện**: test e2e, tối ưu hiệu năng, bảo mật rà soát, tài liệu README/Swagger, Docker deploy.

### Mẫu prompt cho từng phase (copy vào Cursor)
```
Đọc file LIBRARY_MANAGEMENT_SPEC.md. Thực hiện Phase <N>.
Yêu cầu: bám đúng mục <các mục liên quan>, tuân thủ BR-xx, dùng transaction cho nghiệp vụ mượn/trả,
KHÔNG hard-code chính sách (lấy từ settings/reader_types), viết unit test cho service chính,
giao diện admin/client đúng danh sách trang & thành phần đã mô tả. Làm xong liệt kê file đã tạo và
cách chạy/test. Không làm sang phase khác.
```

---
*Hết tài liệu. Mọi con số chính sách là mặc định và phải cấu hình được từ trang Admin → Cấu hình.*
