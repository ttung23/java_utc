# 🧭 CURSOR PLAYBOOK — Backend Java (Spring Boot) + Frontend React, code theo ảnh UI

> Dùng cùng file `LIBRARY_MANAGEMENT_SPEC.md` (nghiệp vụ). File này hướng dẫn **cách giao việc cho Cursor** để ra đầy đủ dự án đúng ảnh UI.
> **Quan trọng:** Mục 2 của SPEC gợi ý NestJS/Node — **BỎ QUA**, dùng stack ở file này. Mọi nghiệp vụ, database, API, BR-xx trong SPEC giữ nguyên.

---

## 1. QUYẾT ĐỊNH CÔNG NGHỆ

| Lớp | Công nghệ | Lý do |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3.x**, Maven | Logic nghiệp vụ chính, transaction/khóa dòng mạnh |
| Bảo mật | Spring Security + JWT (access/refresh), `@PreAuthorize` theo role | RBAC ADMIN/STAFF/USER |
| Dữ liệu | PostgreSQL + Spring Data JPA (Hibernate) + **Flyway** migration | Schema theo SPEC mục 8 |
| Tiện ích | Lombok, MapStruct, Validation (Jakarta), springdoc-openapi (Swagger), Apache POI (Excel), OpenPDF/JasperReports (in phiếu), ZXing (barcode/QR), Spring Mail, `@Scheduled` (cron job) | |
| Test | JUnit 5, Mockito, Testcontainers | |
| Frontend | **React 18 + TypeScript + Vite** | Cursor sinh React từ ảnh UI chính xác nhất, hệ sinh thái lớn |
| UI lib | **Ant Design 5** (admin, nhiều bảng/form) + Tailwind cho tinh chỉnh; client có thể dùng Tailwind + shadcn/ui | Admin cần bảng/lọc/form nhanh |
| State/data | TanStack Query, React Hook Form + Zod, React Router, Zustand | |
| Kết nối | Sinh TypeScript client từ OpenAPI (`orval` hoặc `openapi-typescript`) | FE–BE không lệch kiểu |

> Vue 3 cũng làm được (Element Plus/Naive UI), nhưng chọn **React** cho dự án lớn này vì Cursor/AI hiểu ảnh→code React ổn định hơn và có nhiều thư viện bảng, biểu đồ, quét barcode.

---

## 2. CẤU TRÚC THƯ MỤC & CÁCH ĐẶT ẢNH UI (RẤT QUAN TRỌNG)

```
library-system/
├── docs/
│   ├── LIBRARY_MANAGEMENT_SPEC.md        # nghiệp vụ
│   ├── CURSOR_PROMPT_PLAYBOOK.md         # file này
│   ├── SCREEN_MAP.md                     # bảng ánh xạ ảnh → route → API (mục 4)
│   └── ui/
│       ├── admin/
│       │   ├── 01-login.png
│       │   ├── 02-dashboard.png
│       │   ├── 03-borrow-create-counter.png
│       │   ├── 04-return-book.png
│       │   ├── 05-borrow-online-requests.png
│       │   └── ... (đánh số + tên rõ nghĩa)
│       └── client/
│           ├── 01-home.png
│           ├── 02-book-search.png
│           ├── 03-book-detail.png
│           └── ...
├── backend/                              # Spring Boot
│   └── src/main/java/com/library/
│       ├── common/ (config, security, exception, util, audit)
│       ├── auth/ user/ reader/ card/ book/ copy/ catalog/
│       ├── borrow/ reservation/ fine/ payment/
│       ├── inventory/ imports/ report/ notification/ setting/ news/
│       └── job/                          # @Scheduled
├── admin-web/                            # React + AntD
│   └── src/{app,pages,features,components,services/api,hooks,store,styles,utils}
├── client-web/                           # React + Tailwind
│   └── src/{app,pages,features,components,services/api,hooks,store,styles,utils}
└── docker-compose.yml
```

**Quy tắc đặt tên ảnh:** `NN-ten-man-hinh.png` (NN = số thứ tự). Nếu một màn có nhiều trạng thái, đặt `03a-...-empty.png`, `03b-...-error.png`, `03c-...-modal-confirm.png`. Mỗi ảnh = 1 màn hoặc 1 modal/drawer.

---

## 3. CURSOR RULES (tạo các file trong `.cursor/rules/`)

### `.cursor/rules/00-project.mdc`
```
---
description: Quy tắc tổng cho dự án Library Management
alwaysApply: true
---
- Dự án: hệ thống quản lý thư viện. Nghiệp vụ nằm ở docs/LIBRARY_MANAGEMENT_SPEC.md (BR-xx là bắt buộc).
- Stack: Backend Java 21 + Spring Boot 3 + JPA + Flyway + PostgreSQL. Frontend React 18 + TS + Vite.
  (Bỏ qua phần gợi ý NestJS trong SPEC mục 2.)
- Giao diện phải bám sát ảnh trong docs/ui/** và bảng docs/SCREEN_MAP.md. Không tự đổi layout, màu, bố cục.
- Mọi chính sách (số ngày mượn, mức phạt, hạn mức…) lấy từ bảng settings/reader_types, KHÔNG hard-code.
- Làm đúng phạm vi được giao trong prompt, không tự làm sang module/phase khác.
- Sau mỗi tác vụ: liệt kê file đã tạo/sửa, cách chạy, cách test. Code phải build/chạy được.
- Không bịa API: FE chỉ gọi endpoint có trong OpenAPI của backend (hoặc SPEC mục 9).
- Ngôn ngữ giao diện: tiếng Việt. Ngày DD/MM/YYYY, tiền "1.000đ", múi giờ Asia/Ho_Chi_Minh.
```

### `.cursor/rules/10-backend-java.mdc`
```
---
description: Quy tắc backend Spring Boot
globs: backend/**/*.java
---
- Kiến trúc: Controller (mỏng) → Service (nghiệp vụ, @Transactional) → Repository (Spring Data JPA). DTO request/response tách khỏi Entity (MapStruct).
- Validate bằng Jakarta Validation. Lỗi nghiệp vụ ném BusinessException(code, message) và xử lý bằng @RestControllerAdvice, trả JSON {success,code,message,errors}.
- Response chuẩn {success,data,message,meta}. Phân trang Pageable.
- Mượn/trả/duyệt/thu tiền: bắt buộc @Transactional + khóa bi quan (PESSIMISTIC_WRITE) trên book_copies để chống mượn trùng.
- Phân quyền bằng @PreAuthorize("hasAnyRole('ADMIN','STAFF')"); USER chỉ truy cập dữ liệu của chính mình.
- Mọi thay đổi schema qua Flyway (V__*.sql), không dùng ddl-auto=update.
- Enum dùng @Enumerated(EnumType.STRING). Soft delete cho bảng quan trọng. Auditing created_at/updated_at/created_by.
- Viết unit test cho BorrowPolicyService, FineService, RenewalService, ReservationQueueService.
- Có springdoc-openapi, mỗi endpoint có @Operation + @Tag.
```

### `.cursor/rules/20-frontend-react.mdc`
```
---
description: Quy tắc frontend React
globs: admin-web/**/*.{ts,tsx},client-web/**/*.{ts,tsx}
---
- TypeScript strict. Function component + hooks. Không dùng any.
- Gọi API qua services/api (client sinh từ OpenAPI) + TanStack Query. Không fetch rải rác trong component.
- Form: React Hook Form + Zod. Bảng: phân trang/lọc/sắp xếp phía server.
- Mỗi màn hình phải có: loading (skeleton), empty state, error state, toast thành công/lỗi.
- Tách component dùng lại vào components/; logic theo tính năng vào features/<ten>/.
- Style bám design tokens (src/styles/tokens.ts) trích từ ảnh UI: màu, font, radius, spacing, shadow.
- Route guard theo role. Token lưu an toàn, interceptor tự refresh token.
- Mọi text hiển thị là tiếng Việt. Responsive: admin ≥ 1280px (tablet được), client mobile-first.
```

---

## 4. FILE `docs/SCREEN_MAP.md` (BẠN ĐIỀN — Cursor đọc để biết ảnh nào là màn nào)

Tạo bảng này **trước khi** giao việc. Đây là chìa khóa để Cursor không làm sai/thiếu màn.

```markdown
# SCREEN MAP

## ADMIN
| # | Ảnh | Route | Mục SPEC | API chính (SPEC mục 9) | Quyền | Ghi chú |
|---|---|---|---|---|---|---|
| 01 | ui/admin/01-login.png | /login | 10.14 | POST /auth/login | Public | |
| 02 | ui/admin/02-dashboard.png | / | 10.2 | GET /reports/dashboard | STAFF+ | 8 thẻ KPI + 4 biểu đồ |
| 03 | ui/admin/03-borrow-create-counter.png | /borrows/create | 7.5, 10.3 | GET /readers/lookup, POST /borrows/counter | STAFF+ | Layout 2 cột, quét barcode |
| 04 | ui/admin/04-return-book.png | /returns | 7.7, 10.4 | GET /returns/preview, POST /returns | STAFF+ | |
| ... | | | | | | |

## CLIENT
| # | Ảnh | Route | Mục SPEC | API | Quyền | Ghi chú |
|---|---|---|---|---|---|---|
| 01 | ui/client/01-home.png | / | 11.2 | GET /books/new, /books/popular, /news | Public | |
| ... | | | | | | |
```

> Nếu một ảnh có nhiều thành phần động (tab, modal, dropdown), ghi trong cột Ghi chú. Ảnh nào thiếu so với SPEC → ghi "CHƯA CÓ ẢNH — làm theo mô tả SPEC, dùng đúng design tokens".

---

## 5. QUY TRÌNH LÀM VIỆC (thứ tự bắt buộc)

```
B0  Khởi tạo repo + rules + docs                 ┐
B1  Backend nền: auth, RBAC, schema Flyway, seed │ Backend trước,
B2  Backend từng module nghiệp vụ + test         │ chạy được Swagger
B3  Xuất OpenAPI → sinh TS client                ┘
F0  Trích Design System từ ảnh UI (tokens + component nền)
F1  Layout + Auth + Router (admin & client)
F2  Từng màn hình theo SCREEN_MAP (mỗi lần 1 màn, so với ảnh)
F3  Rà soát: đối chiếu ảnh ↔ giao diện, sửa lệch
F4  Test e2e luồng mượn/trả + hoàn thiện
```
**Mẹo:** làm backend xong từng module rồi mới làm UI module đó (không cần xong cả backend). Trong Cursor luôn **đính kèm ảnh** (kéo thả hoặc `@docs/ui/...png`) vào khung chat khi làm màn tương ứng — Cursor chỉ "nhìn" được ảnh khi bạn đính kèm trong prompt đó. Nên dùng chế độ **Agent** với model mạnh (Claude Sonnet/Opus) cho phần logic và phần ảnh→UI.

---

## 6. PROMPT BACKEND (copy từng prompt)

### B0 — Khởi tạo
```
Đọc docs/LIBRARY_MANAGEMENT_SPEC.md và docs/CURSOR_PROMPT_PLAYBOOK.md, .cursor/rules.
Nhiệm vụ B0: khởi tạo monorepo theo cấu trúc mục 2 của Playbook.
- backend: Spring Boot 3 (Java 21, Maven) với các dependency: web, security, data-jpa, validation, postgresql, flyway, lombok, mapstruct, springdoc-openapi, mail, poi, openpdf, zxing, testcontainers.
- docker-compose.yml chạy PostgreSQL (+ MailHog để test email). application.yml tách profile dev/prod, đọc biến môi trường.
- Thêm cấu hình CORS, Jackson (ISO date, múi giờ Asia/Ho_Chi_Minh), global exception handler, response wrapper chuẩn, Swagger UI.
- Chưa làm nghiệp vụ. Kết thúc: mvn spring-boot:run chạy được, /swagger-ui.html mở được. Liệt kê file đã tạo.
```

### B1 — Schema + Auth + RBAC + Seed
```
Thực hiện B1 theo SPEC mục 3, 8, 9.1, 14.1.
1) Viết Flyway migration V1__init.sql tạo TOÀN BỘ bảng/enum/index/ràng buộc ở SPEC mục 8 (gồm partial unique index cho book_copies ở 8.8).
2) Entity + Repository tương ứng (đặt đúng package theo module).
3) Module auth: register, login, refresh, logout, forgot/reset/change password, verify email, /auth/me. JWT access 15 phút + refresh 7 ngày (lưu hash trong refresh_tokens). Khóa 15 phút sau 5 lần sai.
4) RBAC: ADMIN/STAFF/USER, @PreAuthorize theo ma trận SPEC 3.1. Seed 1 admin, 2 staff.
5) Audit: AOP/Listener ghi audit_logs cho đăng nhập và thay đổi dữ liệu quan trọng.
6) Seed dữ liệu mẫu SPEC 14.1 (class DataSeeder chỉ chạy profile dev).
7) Test: auth flow + phân quyền 403.
Không làm module khác.
```

### B2.x — Từng module (đổi tên module/mục cho từng lần)
```
Thực hiện module <TÊN> theo SPEC: mục <số mục nghiệp vụ> + API mục <9.x> + BR liên quan <BR-..>.
- Controller/Service/Repository/DTO/Mapper đầy đủ, validate, phân quyền đúng ma trận.
- Tìm kiếm/lọc/phân trang theo mô tả SPEC.
- Viết test cho các quy tắc nghiệp vụ.
- Cập nhật Swagger. Cuối cùng liệt kê endpoint đã làm và curl ví dụ.
```
**Thứ tự module khuyên dùng:**
1. `catalog` (categories, authors, publishers, locations, languages) — SPEC M4
2. `book` + `copy` (sinh barcode, tìm full-text, upload bìa, in tem PDF) — M5, M6
3. `reader-type` + `reader` + `card` (+ API `eligibility`) — M3
4. ⭐ `borrow` (xem prompt riêng bên dưới)
5. `reservation` + job hết hạn — M10
6. `fine` + `payment` — M11
7. `notification` (in-app + email + template) — M16
8. `inventory` (supplier, import, audit, disposal) — M13–15
9. `report` + `setting` + `holiday` + `news/banner/review/favorite/suggestion` — M17–21

### B2-BORROW — Module lõi (prompt riêng, kỹ)
```
Thực hiện module BORROW — phần quan trọng nhất của hệ thống. Đọc kỹ SPEC mục 5 (BR-01→BR-36) và mục 7 (7.1→7.11), API 9.4.
Yêu cầu thiết kế:
1) BorrowPolicyService: validateBorrow(reader, copies, context) đúng checklist 7.11; trả danh sách vi phạm có mã lỗi (CARD_EXPIRED, READER_HAS_OVERDUE, UNPAID_FINE_LIMIT, BORROW_LIMIT_EXCEEDED, DUPLICATE_TITLE, COPY_NOT_AVAILABLE, COPY_ON_HOLD, REFERENCE_ONLY…). Hạn mức lấy từ reader_types; chính sách từ settings.
2) DueDateCalculator: tính due_date theo loại độc giả / loan_days_override, dời qua Chủ nhật & holidays (BR-09).
3) BorrowService: createCounterSlip (tạo phiếu quầy, PESSIMISTIC_WRITE trên copy, @Transactional), createOnlineRequest, approve (gán copy, HOLD, pickup_deadline), reject, cancel, handover (đối chiếu barcode, tính due từ ngày giao thực tế).
4) ReturnService: preview (số ngày trễ + phạt dự kiến) và confirm (RETURNED/LOST/DAMAGED, tạo fines, cập nhật copy, kiểm tra hàng đợi reservation → HOLD + READY + thông báo, cập nhật status phiếu PARTIALLY_RETURNED/RETURNED).
5) RenewalService theo BR-16→19 (+ API renew-eligibility trả lý do không đủ điều kiện).
6) FineCalculator: phạt trễ theo ngày × đơn giá, grace_days, cap theo giá bìa, bỏ qua ngày nghỉ nếu cấu hình (BR-25..30).
7) Máy trạng thái phiếu & item đúng SPEC 7.3/7.4 (bọc trong enum + phương thức chuyển trạng thái, không cho chuyển sai).
8) OVERDUE là cờ tính toán, không lưu cứng. Endpoint /borrows/overdue, /borrows/due-soon.
9) In phiếu mượn PDF (A5, có QR mã phiếu).
10) Job: expireUnclaimedSlips (15 phút), dueSoonReminder, overdueReminder.
11) Test: bao phủ toàn bộ 15 test case ở SPEC 14.2 (đặc biệt: mượn đồng thời 1 copy bằng 2 thread — chỉ 1 thành công).
Làm xong liệt kê các lớp, endpoint và kết quả test.
```

### B3 — Xuất OpenAPI cho FE
```
Đảm bảo springdoc xuất /v3/api-docs đầy đủ, có tag theo module, schema DTO rõ ràng, mô tả mã lỗi.
Lưu bản snapshot vào docs/openapi.json. Thêm script ở admin-web và client-web: "gen:api" dùng orval để sinh TypeScript client + hooks TanStack Query vào src/services/api. Chạy thử sinh thành công.
```

---

## 7. PROMPT FRONTEND — ĐÚNG THEO ẢNH UI

### F0 — Trích Design System từ ảnh (làm TRƯỚC khi code màn)
> Đính kèm 4–6 ảnh đại diện (dashboard, một bảng danh sách, một form, một modal, trang chủ client).
```
Đây là các ảnh UI của dự án (đính kèm). Hãy phân tích và trích xuất Design System chính xác:
1) Bảng màu: primary, secondary, success, warning, danger, info, nền, border, text (mã hex), màu badge cho từng trạng thái (SPEC 10.1).
2) Typography: font family (nếu nhận ra), cỡ chữ/độ đậm từng cấp (h1–h6, body, caption, table).
3) Radius, shadow, spacing scale, chiều cao input/button, kích thước sidebar/topbar.
4) Danh sách component dùng lặp lại: Sidebar, Topbar, StatCard, DataTable (toolbar lọc + phân trang), StatusBadge, ConfirmModal, FormSection, EmptyState, BarcodeInput, ReaderInfoCard, BookCard…
Tạo: admin-web/src/styles/tokens.ts (+ cấu hình theme AntD) và client-web/tailwind.config + tokens. Tạo các component nền ở components/ chạy được và 1 trang /__design-system để xem tất cả component. Không làm màn nghiệp vụ.
```

### F1 — Layout + Auth + Router
```
Dựa trên ảnh [đính kèm: layout/sidebar/login] và docs/SCREEN_MAP.md, làm cho admin-web:
- AppLayout (Sidebar theo menu SPEC 10.1, ẩn mục theo role; Topbar có tìm kiếm Ctrl+K, chuông thông báo, avatar menu, dark/light).
- Trang Login đúng ảnh; AuthProvider; axios interceptor refresh token; RouteGuard theo role; trang 403/404.
- Router khai báo đủ tất cả route trong SCREEN_MAP (trang chưa làm hiển thị placeholder "Đang phát triển").
Giao diện phải khớp ảnh. Làm tương tự cho client-web (Header, Footer, đăng nhập/đăng ký, giỏ mượn badge).
```

### F2 — PROMPT MẪU CHO MỖI MÀN HÌNH (copy, thay phần trong <>)
> Đính kèm đúng ảnh của màn đó (và ảnh modal/trạng thái liên quan).
```
Làm màn hình: <TÊN MÀN> trong <admin-web | client-web>.
- Ảnh UI: [đính kèm] <docs/ui/.../NN-ten.png>  (xem dòng #NN trong docs/SCREEN_MAP.md)
- Route: <route>. Quyền: <role>.
- Nghiệp vụ: SPEC mục <x.y>; quy tắc BR: <BR-..>.
- API dùng (đã sinh trong src/services/api): <liệt kê hook/endpoint>.

Yêu cầu:
1) Dựng giao diện KHỚP ẢNH: bố cục, khoảng cách, màu, font, icon, thứ tự cột, nhãn chữ tiếng Việt như ảnh. Dùng component nền/tokens đã có, không tạo style tùy tiện.
2) Gắn API thật: loading/skeleton, empty, error, toast; lọc/sắp xếp/phân trang phía server.
3) Cài đặt hành vi theo SPEC: <liệt kê hành vi chính, ví dụ: quét barcode Enter để thêm, kiểm tra điều kiện realtime, phím tắt F2/F3, in phiếu...>.
4) Validate form bằng Zod, hiển thị thông báo lỗi nghiệp vụ từ mã lỗi backend (map code → thông điệp tiếng Việt).
5) Phần nào ảnh không thể hiện (trạng thái rỗng/lỗi/modal) → tự thiết kế nhất quán với tokens.
6) Cuối cùng: tự đối chiếu với ảnh, liệt kê điểm còn khác biệt (nếu có) và file đã tạo/sửa.
Chỉ làm màn này.
```

**Ví dụ điền cho màn quan trọng nhất — Tạo phiếu mượn tại quầy:**
```
Làm màn hình: Tạo phiếu mượn (quầy) trong admin-web.
- Ảnh UI: [đính kèm] docs/ui/admin/03-borrow-create-counter.png (+ 03a-...-not-eligible.png, 03b-...-print-modal.png)
- Route: /borrows/create. Quyền: STAFF, ADMIN.
- Nghiệp vụ: SPEC 7.5, 7.11, 10.3; BR-01→BR-12.
- API: GET /readers/lookup?card=, GET /readers/:id/eligibility, GET /copies/lookup?barcode=, POST /borrows/validate, POST /borrows/counter, GET /borrows/:id/print.
Yêu cầu: layout 2 cột (trái: ô quét barcode + bảng sách trong phiếu; phải: thẻ độc giả + trạng thái điều kiện + tóm tắt + nút xác nhận) khớp ảnh. Ô quét autofocus, Enter để thêm, beep khi thành công/lỗi. Mỗi lần thêm sách gọi validate và hiện lỗi từng dòng. Phím tắt F2 (độc giả), F3 (barcode), Ctrl+Enter (xác nhận), Esc (hủy). Khi độc giả không đủ điều kiện hiển thị lý do cụ thể + nút "Thu phạt ngay"/"Xem sách quá hạn"; ADMIN có nút override kèm lý do. Sau khi xác nhận mở modal in phiếu (CSS @media print).
Cuối cùng tự đối chiếu với ảnh và liệt kê điểm khác biệt.
```

### F2 — Gom nhóm màn cho nhanh (khi đã quen)
```
Làm cụm màn: <Danh sách độc giả + Form thêm/sửa + Chi tiết độc giả (tabs)>. Ảnh: [đính kèm 3 ảnh]. Làm TUẦN TỰ từng màn, mỗi màn xong chạy được rồi mới sang màn kế; dùng chung DataTable/FormSection. Các yêu cầu như prompt mẫu F2.
```

### F3 — Rà soát lệch giao diện (sau mỗi cụm màn)
```
Đối chiếu giao diện đã code với ảnh [đính kèm ảnh gốc + ảnh chụp màn hình thực tế của tôi].
Liệt kê CHÍNH XÁC mọi khác biệt: khoảng cách, màu, cỡ chữ, thứ tự/nhãn cột, icon, bo góc, trạng thái hover/disabled. Sau đó sửa tất cả. Không đổi logic.
```
> Cách làm: chạy `npm run dev`, chụp màn hình thực tế, dán cùng ảnh gốc cho Cursor. Đây là bước nâng độ giống ảnh nhiều nhất.

### F4 — Hoàn thiện
```
Rà soát toàn dự án: (1) mọi route trong SCREEN_MAP đã có trang chưa; (2) mọi nút/hành động đã gọi API đúng; (3) phân quyền UI theo SPEC 3.1; (4) trạng thái loading/empty/error; (5) responsive; (6) viết Playwright e2e cho 3 luồng: mượn tại quầy → trả sách có phạt; mượn online → duyệt → giao; đặt trước → trả sách → READY. Sửa lỗi và báo cáo.
```

---

## 8. PROMPT SỬA LỖI / GIỮ CHẤT LƯỢNG

**Khi Cursor làm lệch nghiệp vụ**
```
Hành vi này sai so với SPEC mục <x.y> / BR-<n>: <mô tả>. Đọc lại mục đó, sửa service tương ứng, thêm test tái hiện lỗi rồi cho test pass. Không sửa chỗ khác.
```

**Khi Cursor làm quá tay / sang phạm vi khác**
```
Dừng. Chỉ được sửa các file thuộc <module/màn>. Hoàn tác thay đổi ngoài phạm vi (git checkout các file đó) và làm lại đúng yêu cầu.
```

**Khi thiếu ảnh cho một màn**
```
Màn <tên> chưa có ảnh. Hãy làm theo mô tả SPEC mục <x.y>, dùng đúng design tokens và component nền hiện có, bố cục nhất quán với các màn đã làm (ví dụ giống <màn A>). Ghi chú những chỗ bạn tự quyết định.
```

**Chốt tiến độ cuối mỗi phiên**
```
Cập nhật docs/PROGRESS.md: module/màn đã xong, đang dở, còn lại, vấn đề tồn đọng. Commit message gọn theo conventional commits.
```

---

## 9. CHECKLIST TRƯỚC KHI GIAO VIỆC

- [ ] Đã đặt ảnh vào `docs/ui/admin|client` đúng quy tắc đặt tên.
- [ ] Đã điền `docs/SCREEN_MAP.md` (ảnh → route → mục SPEC → API → quyền).
- [ ] Đã chép 3 file rules vào `.cursor/rules/`.
- [ ] Đã đặt `LIBRARY_MANAGEMENT_SPEC.md` + file này vào `docs/`.
- [ ] Đã `git init` và commit sau MỖI phase/màn (để hoàn tác khi Cursor làm sai).
- [ ] Mỗi prompt chỉ giao **một phạm vi nhỏ** (1 module hoặc 1–3 màn); đính kèm đúng ảnh.
- [ ] Chạy thử + test sau mỗi bước, rồi mới sang bước tiếp.

## 10. LƯU Ý THỰC TẾ

1. **Không dán cả SPEC + yêu cầu "làm hết"** — Cursor sẽ mất ngữ cảnh, làm cẩu thả. Luôn chia nhỏ như trên.
2. **Ảnh phải đủ nét**; nếu ảnh quá dài, cắt thành các phần (header / bảng / footer) hoặc chụp riêng modal.
3. **Ảnh chỉ cho thấy giao diện, không cho thấy logic** — logic luôn lấy từ SPEC. Trong prompt phải nêu rõ mục SPEC/BR tương ứng.
4. Phần **logic mượn/trả/phạt** nên review kỹ bằng test; đây là phần dễ sai tinh vi (làm tròn ngày, ngày lễ, đồng thời).
5. Nếu Cursor sinh code quá dài bị cắt giữa chừng → nhắn "tiếp tục từ file đang dở, không viết lại file đã xong".
6. Backend và frontend nên làm ở các cuộc chat (Composer/Agent) **riêng** để ngữ cảnh gọn.
