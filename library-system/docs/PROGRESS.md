# PROGRESS

## Backend — HOÀN THÀNH (B0–B2 + phụ trợ)

- [x] B0/B1 — monorepo, Flyway schema SPEC 8, Auth JWT/RBAC, Audit, Seed
- [x] Catalog / Books / Copies — CRUD, search, barcode, related, cover upload, Excel import/export, PDF tem, copy history
- [x] Readers / Cards / Reader-types — CRUD, eligibility, nested borrowing/history/fines/reservations, Excel import/export
- [x] Borrow/Return/Renew — counter/online/approve/handover/return/renew + PDF phiếu + jobs expire
- [x] Reservation — create/list/me/cancel + expire READY job + notify
- [x] Fine / Payment — list/waive/collect/request/confirm/refund
- [x] Notification — in-app + email best-effort, templates, daily reminders (due/overdue/card)
- [x] Inventory — suppliers, imports, audits, disposals
- [x] Reports — dashboard, overdue (+xlsx), fines, top-books, inventory
- [x] Settings / Holidays / Staff / CMS — news, banners, favorites, reviews, suggestions
- [x] Deps: POI, OpenPDF, ZXing, upload static `/uploads/**`
- [x] CORS patterns cho Vite LAN (`localhost` / `192.168.*` / `10.*`)

## Frontend

### Admin-web
- [x] Core: dashboard, mượn/trả/online/overdue, đặt trước, phạt, thanh toán, báo cáo
- [x] Cards, locations, reader-types, news, banners, profile
- [x] Staff, settings, holidays, audit-logs
- [ ] UI inventory (nhập kho / kiểm kê) — API có, chưa làm màn hình
- [x] OpenAPI snapshot `docs/openapi.json` (129 paths)
- [ ] Sinh TS client từ OpenAPI (optional)

### Client-web
- [x] Scaffold + trang chủ, tra cứu, chi tiết sách, login/register
- [x] Me: borrows, reservations, fines, favorites, notifications, news detail
- [ ] Polish UI / ảnh `docs/ui/**` + e2e

## Ghi chú
- Schema Flyway tối ưu PostgreSQL (`TIMESTAMPTZ`, `jsonb`). Profile `local-h2` không dùng migrate.
- Admin: http://localhost:5173 · Client: http://localhost:5174 · API: http://127.0.0.1:8081
- JDK: máy có thể 17/21; pom `java.version=17`.

## Seed (dev)
| Email | Mật khẩu | Role |
|---|---|---|
| admin@lib.vn | Admin@123 | ADMIN |
| staff1@lib.vn | Staff@123 | STAFF |
| reader1@lib.vn | User@123 | USER |
