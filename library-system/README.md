# Library Management System

Hệ thống quản lý thư viện theo `docs/LIBRARY_MANAGEMENT_SPEC.md` + `docs/CURSOR_PROMPT_PLAYBOOK.md`.

**Stack:** Java 21 + Spring Boot + PostgreSQL + Flyway · React 18 + TypeScript + Vite (admin-web / client-web).

> Project cũ `baitaplon` (SLMS) giữ nguyên. Code mới nằm ở thư mục này.

## Cấu trúc

```
library-system/
├── docs/                 # SPEC, Playbook, SCREEN_MAP, PROGRESS, ui/
├── backend/              # Spring Boot API
├── admin-web/            # React Admin (F0+)
├── client-web/           # React Client (F0+)
└── docker-compose.yml    # PostgreSQL + MailHog
```

## Phase hiện tại: **B0** (nền tảng)

Chưa có nghiệp vụ. Có health API + Swagger + CORS + exception handler + Flyway placeholder.

## Chạy nhanh (không cần Docker — H2)

```bash
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-h2" "-Dspring-boot.run.arguments=--server.port=8081"
```

- Health: http://localhost:8081/api/v1/health  
- Swagger: http://localhost:8081/swagger-ui.html  

(Port `8080` có thể đang bị project `baitaplon` chiếm.)

## Chạy với PostgreSQL (đúng profile `dev`)

```bash
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

## Bước tiếp theo (Playbook)

**B1** — Flyway schema đầy đủ (SPEC mục 8) + Auth JWT + RBAC + Seed.

Đặt ảnh UI vào `docs/ui/admin|client` rồi cập nhật `docs/SCREEN_MAP.md`.
