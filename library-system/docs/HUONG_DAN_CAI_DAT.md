# Hướng dẫn cài đặt & chạy (đồng nghiệp)

Sau khi `git pull`, làm theo thứ tự dưới đây để chạy **cả 3 phần**: Backend + **Admin** + **Client**.

> Máy **Windows** (PowerShell / CMD, Docker hoặc Postgres native): xem [`HUONG_DAN_CAI_DAT_WINDOWS.md`](./HUONG_DAN_CAI_DAT_WINDOWS.md).

## Yêu cầu máy

| Công cụ | Gợi ý |
|---|---|
| JDK | 17 hoặc 21 |
| Node.js | 18+ (khuyến nghị 20 LTS) |
| Docker Desktop | Bật trước khi `docker compose` |
| Git | — |

## Cấu trúc

```
library-system/
├── backend/        # Spring Boot API
├── admin-web/      # Quản trị (STAFF/ADMIN)  → http://localhost:5173
├── client-web/     # Cổng độc giả (USER)     → http://localhost:5174
├── docker-compose.yml
└── docs/
```

> **Quan trọng:** Frontend (admin + client) proxy `/api` tới **`http://127.0.0.1:8081`**.  
> Vì vậy backend nên chạy cổng **8081**.

## 1. Database + Mailhog

Trong thư mục `library-system/`:

```bash
docker compose up -d
docker compose ps
```

| Service | Port | Ghi chú |
|---|---|---|
| PostgreSQL | `5432` | DB `library` / user `library` / pass `library` |
| Mailhog SMTP | `1025` | Email dev |
| Mailhog UI | http://localhost:8025 | Xem mail gửi thử |

## 2. Backend API

```bash
cd library-system/backend
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Lần đầu Maven tải dependency (có thể vài phút). Khi thấy `Started LibraryApiApplication` là OK.

| URL | Mục đích |
|---|---|
| http://localhost:8081/api/v1/health | Health check |
| http://localhost:8081/swagger-ui.html | Swagger |
| http://localhost:8081/v3/api-docs | OpenAPI JSON |

Profile mặc định: `dev` → kết nối Postgres + chạy **DataSeeder** (user/sách mẫu).

### Tài khoản seed

| Email | Mật khẩu | Dùng cho |
|---|---|---|
| `admin@lib.vn` | `Admin@123` | **Admin-web** |
| `staff1@lib.vn` | `Staff@123` | **Admin-web** |
| `reader1@lib.vn` | `User@123` | **Client-web** |

## 3. Admin-web (quản trị)

Terminal mới:

```bash
cd library-system/admin-web
cp .env.example .env    # lần đầu
npm install
npm run dev
```

- URL: **http://localhost:5173**
- Đăng nhập bằng `admin@lib.vn` / `Admin@123` (hoặc staff)
- Proxy: trình duyệt gọi `/api/v1/...` → Vite chuyển tới BE `:8081`

Khuyến nghị mở bằng `localhost` (không dùng IP LAN) để tránh lỗi CORS cũ; BE hiện đã nới CORS LAN.

## 4. Client-web (cổng độc giả)

Terminal mới:

```bash
cd library-system/client-web
cp .env.example .env    # lần đầu (nếu có)
npm install
npm run dev
```

- URL: **http://localhost:5174**
- Đăng nhập bằng `reader1@lib.vn` / `User@123`
- Public không cần login: trang chủ, tra cứu sách, tin tức
- Sau login: mượn online, đặt trước, yêu thích, phạt, thông báo

### Luồng nhanh phía client

1. Mở http://localhost:5174 → Tra cứu  
2. Vào chi tiết sách → **Mượn online** / **Đặt trước** / **Yêu thích**  
3. Menu **Mượn / Đặt trước / Phạt / Thông báo** để xem dữ liệu của mình  
4. Trên **Admin** (`5173`): **Yêu cầu mượn online** → Duyệt → Giao sách  

## Thứ tự chạy khuyến nghị

```text
1) docker compose up -d
2) SERVER_PORT=8081 ./mvnw spring-boot:run     (backend/)
3) npm run dev                                   (admin-web/)   → :5173
4) npm run dev                                   (client-web/)  → :5174
```

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Login admin `500` / proxy error | BE chưa chạy hoặc sai cổng | Chạy BE `SERVER_PORT=8081` |
| Login `403` Invalid CORS | Mở FE bằng IP LAN + BE cũ | Dùng `http://localhost:5173` hoặc pull BE mới (CORS patterns) |
| Flyway / DB fail | Postgres chưa lên | `docker compose up -d`, đợi healthy |
| `local-h2` fail `TIMESTAMPTZ` | Schema Postgres-only | **Không** dùng profile H2; dùng Docker Postgres |
| Port 8081 đang dùng | Process cũ | `lsof -i :8081` rồi kill, chạy lại |
| Client không gọi được API | Quên BE hoặc chạy sai port | Health: `curl http://127.0.0.1:8081/api/v1/health` |

## Kiểm tra nhanh sau khi lên

```bash
curl -s http://127.0.0.1:8081/api/v1/health
curl -s -X POST http://127.0.0.1:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"reader1@lib.vn","password":"User@123"}'
```

- Admin UI: http://localhost:5173  
- **Client UI: http://localhost:5174**  
- Mailhog: http://localhost:8025  

## Tài liệu liên quan

- Nghiệp vụ: [`NGHIEP_VU.md`](./NGHIEP_VU.md)
- Tiến độ: [`PROGRESS.md`](./PROGRESS.md)
- Spec đầy đủ: [`LIBRARY_MANAGEMENT_SPEC.md`](./LIBRARY_MANAGEMENT_SPEC.md)
- Bản đồ màn hình: [`SCREEN_MAP.md`](./SCREEN_MAP.md)
