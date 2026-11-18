# PROGRESS

## Đã xong
- [x] **B0** — Khởi tạo monorepo, docs, `.cursor/rules`, Spring Boot nền, docker-compose, placeholder FE
- [x] **B1** — Schema Flyway V1 (SPEC mục 8) + Entities/Repos + Auth JWT + RBAC + Audit + DataSeeder (profile `dev`) + kiểm tra login/403
- [x] **B2 catalog** — CRUD `/categories`, `/authors`, `/publishers`, `/locations`, `/languages`, `/document-types` (GET public; ghi STAFF/ADMIN; soft-delete; tree categories; `CatalogApiTest`)
- [x] **B2 book/copy (core)** — Books + copies APIs SPEC 9.3 (search/filter, detail+copySummary, CRUD soft-delete, new/popular, add copies + barcode `BC-{bookId}-{copyNo}`, lookup/list/patch copies). Skip: import/export, cover upload, PDF labels, related.
- [x] **B2 reader/card (core)** — `/reader-types` CRUD (GET STAFF+, ghi ADMIN); `/readers` list/me/lookup/detail/create/update/block/unblock/blacklist + `/eligibility` (không borrow); `/cards` list/issue/renew/report-lost. Skip: import Excel, nested borrowing/history/fines/reservations (trả list rỗng).
- [x] **F0/F1 admin-web** — Vite+React+AntD: login JWT, layout sidebar SPEC 10.1, dashboard, catalog gắn API, route placeholder còn lại
- [x] **F2 partial** — Trang `/books` + `/copies` + `/catalog` gắn API thật
- [x] **F2 readers UI** — `/readers`, `/readers/:id` (+ eligibility)

## Đang dở
- (không)

## Còn lại (theo Playbook)
- [ ] B2-BORROW — Module mượn/trả (lõi)
- [ ] B2.x — reservation → fine…
- [ ] B3 — OpenAPI → TS client
- [ ] F2–F4 — Các màn còn lại + client-web

## Tồn đọng
- Chưa có ảnh UI trong `docs/ui/**` — SCREEN_MAP ghi chú CHƯA CÓ ẢNH; khi có ảnh cần cập nhật và làm F0 theo ảnh.
- Project cũ `baitaplon` (SLMS) giữ nguyên; hệ thống mới nằm ở `library-system/`.
- Playbook yêu cầu Java 21; máy hiện tại JDK 17 → backend dùng Java 17 + Spring Boot 3.4.1 (nâng JDK khi sẵn sàng).
- Port 8080 có thể bị SLMS cũ chiếm — dùng `SERVER_PORT=8081` hoặc tắt process cũ.

## Tài khoản seed (dev)
| Email | Mật khẩu | Role |
|---|---|---|
| admin@lib.vn | Admin@123 | ADMIN |
| staff1@lib.vn / staff2@lib.vn | Staff@123 | STAFF |
| reader1@lib.vn … reader20@lib.vn | User@123 | USER |
