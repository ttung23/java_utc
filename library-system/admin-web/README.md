# admin-web

React 18 + TypeScript + Vite + Ant Design 5 — giao diện quản trị thư viện.

## Chạy

Yêu cầu backend đang chạy (mặc định proxy tới `http://localhost:8081`).

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
