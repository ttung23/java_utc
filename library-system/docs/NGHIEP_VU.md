# Nghiệp vụ hệ thống quản lý thư viện

Tài liệu tóm tắt nghiệp vụ theo code hiện tại (admin-web + **client-web** + backend).  
Chi tiết luật BR / API đầy đủ: `LIBRARY_MANAGEMENT_SPEC.md`.

---

## 1. Vai trò

| Role | Ứng dụng | Việc chính |
|---|---|---|
| **ADMIN** | Admin `5173` | Toàn quyền + cấu hình, NV, loại ĐG, audit, miễn phạt |
| **STAFF** | Admin `5173` | Quầy mượn/trả, duyệt online, độc giả, sách, phạt/thu tiền |
| **USER** (độc giả) | **Client `5174`** | Tra cứu, mượn online, đặt trước, xem phạt/thông báo |

---

## 2. Phía Client (cổng độc giả) — http://localhost:5174

Đây là portal cho độc giả, **không** dùng admin-web.

### 2.1 Công khai (chưa login)

| Màn | Route | Việc làm |
|---|---|---|
| Trang chủ | `/` | Sách mới, phổ biến, banner, tin tức |
| Tra cứu | `/books` | Tìm sách theo tên / ISBN |
| Chi tiết sách | `/books/:id` | Xem mô tả, đánh giá, sách liên quan |
| Tin tức | `/news/:slug` | Đọc tin |
| Đăng nhập / Đăng ký | `/login`, `/register` | Tạo phiên USER |

### 2.2 Sau khi đăng nhập (`reader1@lib.vn`)

| Màn | Route | Việc làm |
|---|---|---|
| Mượn của tôi | `/me/borrows` | Xem phiếu online/quầy; hủy khi PENDING/APPROVED |
| Đặt trước | `/me/reservations` | Xem hàng đợi / READY; hủy |
| Phạt | `/me/fines` | Xem nợ; gửi yêu cầu nộp (`payments/request`) |
| Yêu thích | `/me/favorites` | Danh sách sách đã tim |
| Thông báo | `/me/notifications` | Đọc / đánh dấu đã đọc |

### 2.3 Thao tác trên trang chi tiết sách

1. **Mượn online** → `POST /borrows/online` (theo `bookId`)  
   - Phiếu `PENDING` chờ thủ thư duyệt trên Admin.  
2. **Đặt trước** → `POST /reservations`  
   - Khi hết sách AVAILABLE; vào hàng đợi.  
3. **Yêu thích** → `POST/DELETE /favorites/{bookId}`  
4. **Đánh giá** → `POST /reviews` (rating + comment)

### 2.4 Luồng end-to-end Client ↔ Admin

```text
[Client] Độc giả chọn sách → Mượn online
    ↓
[BE] Tạo phiếu channel=ONLINE, status=PENDING
    ↓
[Admin] Mượn–Trả → Yêu cầu mượn online → Duyệt
    ↓
[Admin] Giao sách (quét barcode bản sao AVAILABLE)
    ↓
[BE] status=BORROWING, copy → BORROWED, có dueDate
    ↓
[Client] /me/borrows thấy đang mượn
    ↓
[Admin] Trả sách (preview phạt nếu quá hạn) → confirm
```

---

## 3. Phía Admin — http://localhost:5173

### 3.1 Mượn – Trả

| Màn | Route | Nghiệp vụ |
|---|---|---|
| Tạo phiếu quầy | `/borrows/create` | Quét thẻ ĐG + barcode copy → `POST /borrows/counter` |
| Trả sách | `/returns` | Preview phạt → confirm điều kiện sách |
| Yêu cầu online | `/borrows/online` | Duyệt / Từ chối / Giao sách |
| Danh sách phiếu | `/borrows` | Lọc status/channel; in PDF |
| Quá hạn | `/borrows/overdue` | Item quá hạn |
| Đặt trước | `/reservations` | Quản lý / hủy |

**Khái niệm trạng thái phiếu (tóm tắt):**  
`PENDING` → `APPROVED` → `BORROWING` → `RETURNED` (hoặc `CANCELLED` / `REJECTED` / `EXPIRED`).

### 3.2 Độc giả & thẻ

| Màn | Route | Nghiệp vụ |
|---|---|---|
| Độc giả | `/readers`, `/readers/:id` | CRUD / lookup thẻ / lịch sử mượn-phạt |
| Thẻ | `/cards` | Cấp thẻ, gia hạn, báo mất |
| Loại ĐG | `/reader-types` | Quota: max sách, ngày mượn, gia hạn, đặt trước (ADMIN) |

### 3.3 Kho sách

| Màn | Route | Nghiệp vụ |
|---|---|---|
| Đầu sách | `/books` | CRUD, bìa, import/export Excel |
| Bản sao | `/copies` | Barcode, trạng thái AVAILABLE/BORROWED/… |
| Catalog | `/catalog` | Thể loại / tác giả / NXB |
| Vị trí kệ | `/locations` | Mã kệ / zone / tầng |

### 3.4 Phạt & thu tiền

| Màn | Route | Nghiệp vụ |
|---|---|---|
| Khoản phạt | `/fines` | List, tạo tay, miễn/giảm (ADMIN) |
| Biên lai | `/payments` | Thu tiền quầy, xác nhận yêu cầu nộp từ client, hoàn (ADMIN) |

Phạt phát sinh khi: quá hạn trả, hỏng/mất (theo điều kiện return).

### 3.5 Nội dung & hệ thống

| Màn | Route | Ai dùng |
|---|---|---|
| Tin tức / Banner | `/news`, `/banners` | STAFF+ (xóa: ADMIN) |
| Báo cáo | `/reports` | KPI, quá hạn Excel, top sách, tồn kho |
| Nhân viên / Settings / Holidays / Audit | `/staff`… | ADMIN |
| Hồ sơ | `/profile` | Đổi mật khẩu |

---

## 4. Quy tắc nghiệp vụ cốt lõi (tóm tắt)

1. **Chỉ copy AVAILABLE** mới mượn / giao được.  
2. **Online** mượn theo **bookId** (chưa gắn copy); **handover** mới gán barcode.  
3. **Quota** theo loại độc giả: số sách đang mượn, số đặt trước, số lần gia hạn.  
4. **Thẻ ACTIVE** và ĐG không blacklist mới đủ điều kiện mượn.  
5. **Quá hạn** → tính phạt theo settings + ngày nghỉ (holidays).  
6. **Đặt trước**: khi có sẵn → READY + thời hạn lấy; hết hạn READY → nhả cho người sau / AVAILABLE.  
7. **Client** chỉ thao tác dữ liệu “của tôi” (`/me`, `/borrows/me`, …). Admin thao tác toàn hệ thống.

---

## 5. API nhóm theo phía Client

| Việc | Method | Path |
|---|---|---|
| Login / register | POST | `/auth/login`, `/auth/register` |
| Sách | GET | `/books`, `/books/{id}`, `/books/new`, `/books/popular` |
| Mượn online | POST | `/borrows/online` |
| Phiếu của tôi | GET | `/borrows/me` |
| Đặt trước | POST/GET | `/reservations`, `/reservations/me` |
| Phạt / xin nộp | GET/POST | `/fines/me`, `/payments/request` |
| Yêu thích | GET/POST/DELETE | `/favorites/me`, `/favorites/{bookId}` |
| Thông báo | GET/PATCH | `/notifications/me`, `/notifications/{id}/read` |
| Review / tin | GET/POST | `/reviews`, `/news/public` |

Base path: `/api/v1`. Swagger: http://localhost:8081/swagger-ui.html

---

## 6. Demo nhanh 5 phút (có Client)

1. BE `:8081` + Admin `:5173` + **Client `:5174`** đang chạy.  
2. Client login `reader1@lib.vn` / `User@123`.  
3. Vào 1 cuốn sách → **Mượn online**.  
4. Admin login `admin@lib.vn` → **Yêu cầu mượn online** → Duyệt → Giao sách (barcode AVAILABLE từ seed).  
5. Client mở **Mượn của tôi** thấy phiếu `BORROWING`.  
6. Admin **Trả sách** bằng barcode → xong vòng đời.

---

## 7. Còn thiếu (biết để không nhầm)

- Admin: màn **inventory** (nhập kho / kiểm kê) — API có, UI chưa.  
- Client: giỏ mượn nhiều sách (`/borrow-cart` trong SCREEN_MAP) chưa tách màn riêng (đang mượn 1 cuốn từ trang chi tiết).  
- Ảnh UI `docs/ui/**` + e2e chưa có.

Xem thêm: [`HUONG_DAN_CAI_DAT.md`](./HUONG_DAN_CAI_DAT.md) · [`SCREEN_MAP.md`](./SCREEN_MAP.md) · [`PROGRESS.md`](./PROGRESS.md)
