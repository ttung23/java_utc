# Hướng dẫn cài đặt & chạy trên Windows

Hướng dẫn dành cho máy **Windows 10/11**: cài môi trường → pull code → chạy Backend + Admin + Client.

Có **2 cách** cho database:

| Cách | Khi nào dùng |
|---|---|
| **A — Có Docker** | Khuyến nghị: Postgres + Mailhog chạy bằng `docker compose` |
| **B — Chưa có Docker** | Cài PostgreSQL trực tiếp trên Windows |

> **Quan trọng:** Frontend proxy `/api` tới `http://127.0.0.1:8081`.  
> Backend **phải** chạy cổng **8081** (không dùng mặc định 8080).

---

## 0. Cài môi trường (làm một lần)

### 0.1. Checklist

| Công cụ | Phiên bản | Bắt buộc? | Ghi chú |
|---|---|---|---|
| Git | mới nhất | Có | Clone / pull |
| JDK | **17** hoặc **21** | Có | Spring Boot 3 |
| Node.js | **18+** (khuyến nghị **20 LTS**) | Có | Admin + Client |
| Docker Desktop | mới nhất | Chỉ cách A | Postgres + Mailhog |
| PostgreSQL | **16** (hoặc 14+) | Chỉ cách B | Cài native nếu không dùng Docker |
| Terminal | PowerShell / CMD / Git Bash | Có | Khuyến nghị **PowerShell** hoặc **Windows Terminal** |

### 0.2. Git

1. Tải: https://git-scm.com/download/win  
2. Cài với tùy chọn mặc định (Git Bash + thêm Git vào PATH).  
3. Kiểm tra:

```powershell
git --version
```

### 0.3. JDK (Java)

1. Tải JDK 17 hoặc 21 (Temurin / Oracle / Microsoft Build of OpenJDK).  
   - Ví dụ Temurin: https://adoptium.net/  
2. Cài đặt, ghi nhớ thư mục (ví dụ `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`).  
3. Thêm biến môi trường:

| Tên | Giá trị |
|---|---|
| `JAVA_HOME` | Đường dẫn thư mục JDK (không gồm `\bin`) |
| `Path` | Thêm `%JAVA_HOME%\bin` |

Cách mở: **Settings → System → About → Advanced system settings → Environment Variables**.

4. **Mở lại** PowerShell rồi kiểm tra:

```powershell
java -version
javac -version
echo $env:JAVA_HOME
```

Phải thấy Java 17 hoặc 21.

### 0.4. Node.js

1. Tải LTS: https://nodejs.org/  
2. Cài (tick thêm PATH nếu có).  
3. Kiểm tra:

```powershell
node -v
npm -v
```

`node` nên ≥ `v18`.

### 0.5. (Cách A) Docker Desktop

1. Tải: https://www.docker.com/products/docker-desktop/  
2. Cài, khởi động lại máy nếu được yêu cầu.  
3. Bật **WSL 2** nếu Docker nhắc (Windows Features → Virtual Machine Platform / WSL).  
4. Mở **Docker Desktop**, đợi icon whale chạy (Engine running).  
5. Kiểm tra:

```powershell
docker --version
docker compose version
```

### 0.6. (Cách B) PostgreSQL native — chỉ khi không dùng Docker

1. Tải PostgreSQL 16: https://www.postgresql.org/download/windows/  
   (Installer từ EnterpriseDB).  
2. Trong wizard:
   - Port: **5432**
   - Superuser `postgres`: đặt mật khẩu và **ghi nhớ** (ví dụ `postgres`)
   - Có thể bỏ chọn Stack Builder nếu không cần  
3. Đảm bảo service đang chạy: **Services** (`services.msc`) → `postgresql-x64-16` → Status = Running.  
4. Thêm `bin` vào PATH nếu chưa có, ví dụ:  
   `C:\Program Files\PostgreSQL\16\bin`  
5. Kiểm tra:

```powershell
psql --version
```

### 0.7. Pull / clone dự án

```powershell
# Clone lần đầu (đổi URL theo repo thật của nhóm)
git clone <URL_REPO> java_utc
cd java_utc\library-system

# Hoặc nếu đã có code
cd path\to\library-system
git pull
```

Cấu trúc cần thấy:

```text
library-system/
├── backend/           # Spring Boot API
├── admin-web/         # Quản trị → http://localhost:5173
├── client-web/        # Độc giả  → http://localhost:5174
├── docker-compose.yml
└── docs/
```

---

## Cách A — Có Docker (khuyến nghị)

### A1. Chạy Postgres + Mailhog

Mở PowerShell trong thư mục `library-system`:

```powershell
cd path\to\library-system
docker compose up -d
docker compose ps
```

| Service | Port | Ghi chú |
|---|---|---|
| PostgreSQL | `5432` | DB `library` / user `library` / pass `library` |
| Mailhog SMTP | `1025` | Email dev |
| Mailhog UI | http://localhost:8025 | Xem mail gửi thử |

Đợi Postgres **healthy** rồi mới chạy backend.

Dừng / xóa container (khi cần):

```powershell
docker compose down
# Xóa luôn data volume (reset DB sạch):
docker compose down -v
```

### A2. Backend (cổng 8081)

**PowerShell** (terminal mới):

```powershell
cd path\to\library-system\backend
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

**CMD**:

```cmd
cd path\to\library-system\backend
set SERVER_PORT=8081
mvnw.cmd spring-boot:run
```

**Git Bash**:

```bash
cd path/to/library-system/backend
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Lần đầu Maven tải dependency (có thể vài phút). Khi thấy dòng tương tự:

```text
Started LibraryApiApplication
```

là OK.

| URL | Mục đích |
|---|---|
| http://localhost:8081/api/v1/health | Health check |
| http://localhost:8081/swagger-ui.html | Swagger UI |
| http://localhost:8081/v3/api-docs | OpenAPI JSON |

Profile mặc định: `dev` → kết nối Postgres + chạy seeder (user/sách mẫu).

### A3. Admin-web

Terminal mới:

```powershell
cd path\to\library-system\admin-web
copy .env.example .env
npm install
npm run dev
```

- URL: **http://localhost:5173**  
- Đăng nhập: `admin@lib.vn` / `Admin@123`  
- Mở bằng `localhost` (không dùng IP LAN nếu không cần)

### A4. Client-web

Terminal mới:

```powershell
cd path\to\library-system\client-web
copy .env.example .env
npm install
npm run dev
```

- URL: **http://localhost:5174**  
- Đăng nhập: `reader1@lib.vn` / `User@123`

### A5. Thứ tự chạy (Cách A)

```text
1) Mở Docker Desktop (Engine running)
2) docker compose up -d
3) Backend:  $env:SERVER_PORT="8081"; .\mvnw.cmd spring-boot:run
4) Admin:    npm run dev   → :5173
5) Client:   npm run dev   → :5174
```

Cần **3–4 cửa sổ terminal** mở song song (Docker đã chạy nền).

---

## Cách B — Chưa có Docker (PostgreSQL trên Windows)

Mailhog **không bắt buộc**. App vẫn chạy; chỉ không xem được UI mail tại `:8025`.

> **Không** dùng profile `local-h2`. Schema dự án gắn PostgreSQL (`TIMESTAMPTZ`…), H2 dễ fail Flyway.

### B1. Tạo database `library`

Mở **SQL Shell (psql)** hoặc PowerShell:

```powershell
psql -U postgres
```

Trong `psql`:

```sql
CREATE USER library WITH PASSWORD 'library';
CREATE DATABASE library OWNER library;
GRANT ALL PRIVILEGES ON DATABASE library TO library;
\q
```

Nếu báo user/DB đã tồn tại thì bỏ qua lệnh tương ứng.

Kiểm tra kết nối:

```powershell
psql -U library -d library -h localhost
# nhập password: library
```

### B2. Backend

Giống Cách A (cổng **8081**). Config mặc định đã khớp:

| Biến | Mặc định |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/library` |
| `DB_USER` | `library` |
| `DB_PASSWORD` | `library` |

**PowerShell:**

```powershell
cd path\to\library-system\backend
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

Nếu Postgres của bạn khác user/pass/port:

```powershell
$env:SERVER_PORT = "8081"
$env:DB_URL = "jdbc:postgresql://localhost:5432/library"
$env:DB_USER = "library"
$env:DB_PASSWORD = "library"
.\mvnw.cmd spring-boot:run
```

Mail host mặc định `localhost:1025`. Không có Mailhog thì một số chức năng gửi mail có thể log lỗi SMTP — **không chặn** login / API chính.

### B3. Admin + Client

Giống Cách A:

```powershell
# Admin
cd path\to\library-system\admin-web
copy .env.example .env
npm install
npm run dev

# Client (terminal khác)
cd path\to\library-system\client-web
copy .env.example .env
npm install
npm run dev
```

### B4. Thứ tự chạy (Cách B)

```text
1) Service PostgreSQL đang Running
2) Đã có DB/user library (B1)
3) Backend :8081
4) Admin :5173
5) Client :5174
```

---

## Tài khoản demo (sau khi BE start + seed)

| Email | Mật khẩu | Dùng cho |
|---|---|---|
| `admin@lib.vn` | `Admin@123` | Admin-web |
| `staff1@lib.vn` | `Staff@123` | Admin-web |
| `reader1@lib.vn` | `User@123` | Client-web |

---

## Kiểm tra nhanh sau khi lên

PowerShell:

```powershell
Invoke-RestMethod http://127.0.0.1:8081/api/v1/health

Invoke-RestMethod -Method POST http://127.0.0.1:8081/api/v1/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"reader1@lib.vn","password":"User@123"}'
```

Trình duyệt:

| App | URL |
|---|---|
| Backend health | http://localhost:8081/api/v1/health |
| Swagger | http://localhost:8081/swagger-ui.html |
| Admin | http://localhost:5173 |
| Client | http://localhost:5174 |
| Mailhog (chỉ Cách A) | http://localhost:8025 |

---

## Lỗi thường gặp (Windows)

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `java` / `javac` không nhận | Chưa set `JAVA_HOME` / PATH | Set env → **mở lại** terminal |
| `.\mvnw.cmd` báo lỗi permission / không chạy | Sai thư mục hoặc antivirus | `cd backend`, chạy lại; thử `cmd /c mvnw.cmd -v` |
| `docker compose` fail / cannot connect | Docker Desktop chưa chạy | Mở Docker Desktop, đợi Engine ready |
| Flyway / connection refused `:5432` | Postgres chưa lên | Cách A: `docker compose ps`. Cách B: kiểm tra Services PostgreSQL |
| Auth fail Postgres `library` | Sai user/pass hoặc chưa tạo DB | Làm lại bước B1; hoặc dùng Docker (Cách A) |
| Login FE lỗi proxy / `ECONNREFUSED` | BE chưa chạy hoặc sai cổng | BE phải `:8081` |
| Port `8081` đang dùng | Process cũ | `netstat -ano \| findstr :8081` → `taskkill /PID <pid> /F` |
| Port `5432` conflict | Postgres local + Docker cùng port | Tắt một bên, hoặc đổi port map Docker |
| `npm install` chậm / fail SSL | Mạng / proxy công ty | Thử mạng khác; hoặc `npm config set registry https://registry.npmjs.org` |
| Dùng profile `local-h2` bị lỗi schema | H2 không tương thích đủ | **Không dùng** H2 — dùng Postgres |
| Mở FE bằng IP LAN bị CORS | Origin lạ | Dùng `http://localhost:5173` / `5174` |

### Giải phóng cổng 8081 (ví dụ)

```powershell
netstat -ano | findstr :8081
taskkill /PID <PID_HIỆN_RA> /F
```

---

## Ghi chú PowerShell vs CMD vs Git Bash

| Việc | PowerShell | CMD | Git Bash |
|---|---|---|---|
| Set port BE | `$env:SERVER_PORT="8081"` | `set SERVER_PORT=8081` | `SERVER_PORT=8081` (cùng dòng lệnh) |
| Maven wrapper | `.\mvnw.cmd` | `mvnw.cmd` | `./mvnw` |
| Copy env FE | `copy .env.example .env` | `copy .env.example .env` | `cp .env.example .env` |

Biến `$env:...` trong PowerShell **chỉ có hiệu lực** trong cửa sổ terminal đó.

---

## Tài liệu liên quan

- Cài đặt chung (macOS/Linux style): [`HUONG_DAN_CAI_DAT.md`](./HUONG_DAN_CAI_DAT.md)
- Nghiệp vụ: [`NGHIEP_VU.md`](./NGHIEP_VU.md)
- Tiến độ: [`PROGRESS.md`](./PROGRESS.md)
- Spec: [`LIBRARY_MANAGEMENT_SPEC.md`](./LIBRARY_MANAGEMENT_SPEC.md)
- Bản đồ màn hình: [`SCREEN_MAP.md`](./SCREEN_MAP.md)
