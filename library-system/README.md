# Library Management System

Hệ thống quản lý thư viện theo `docs/LIBRARY_MANAGEMENT_SPEC.md` + `docs/CURSOR_PROMPT_PLAYBOOK.md`.

**Stack:** Java 17/21 + Spring Boot 3 + PostgreSQL + Flyway · React 18 + TypeScript + Vite (admin-web / client-web).

## Tài liệu cho đồng nghiệp

| File | Nội dung |
|---|---|
| [`docs/HUONG_DAN_CAI_DAT.md`](docs/HUONG_DAN_CAI_DAT.md) | Pull → Docker → BE → **Admin** → **Client** |
| [`docs/HUONG_DAN_CAI_DAT_WINDOWS.md`](docs/HUONG_DAN_CAI_DAT_WINDOWS.md) | **Windows**: cài JDK/Node/Git + có Docker / chưa Docker |
| [`docs/NGHIEP_VU.md`](docs/NGHIEP_VU.md) | Nghiệp vụ admin + **phía client** |
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Tiến độ |

## Chạy nhanh (3 app)

```bash
# 1) DB
docker compose up -d

# 2) API (bắt buộc cổng 8081 vì FE proxy)
cd backend && SERVER_PORT=8081 ./mvnw spring-boot:run

# 3) Admin — http://localhost:5173
cd admin-web && npm install && npm run dev

# 4) Client (độc giả) — http://localhost:5174
cd client-web && npm install && npm run dev
```

| App | URL | Tài khoản demo |
|---|---|---|
| Backend | http://localhost:8081 | Swagger: `/swagger-ui.html` |
| **Admin-web** | http://localhost:5173 | `admin@lib.vn` / `Admin@123` |
| **Client-web** | http://localhost:5174 | `reader1@lib.vn` / `User@123` |

Chi tiết / troubleshooting: `docs/HUONG_DAN_CAI_DAT.md`.
