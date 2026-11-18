# admin-web

React 18 + TypeScript + Vite + Ant Design 5 — giao diện quản trị thư viện.

## Chạy

Yêu cầu backend đang chạy (Vite proxy `/api` → `http://127.0.0.1:8081`).
Chạy BE: `cd backend && SERVER_PORT=8081 ./mvnw spring-boot:run` (cần `docker compose up -d` trước).

```bash
cd admin-web
npm install
npm run dev
```

Mở: http://localhost:5173

Đăng nhập demo: `admin@lib.vn` / `Admin@123` (STAFF/ADMIN). USER bị từ chối vào admin.

## Đã có (F0/F1)
- Login + JWT (localStorage) + refresh interceptor
- Layout sidebar theo SPEC 10.1, ẩn menu ADMIN
- Dashboard + trang Catalog gắn API `/authors|/publishers|/categories`
- Các route SCREEN_MAP còn lại: placeholder “Đang phát triển”
