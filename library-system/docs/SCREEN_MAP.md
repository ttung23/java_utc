# SCREEN MAP

> Ảnh UI: **CHƯA CÓ** trong `docs/ui/**`. Các màn dưới đây làm theo mô tả SPEC + design tokens khi có (Playbook mục 8: thiếu ảnh → theo SPEC).
> Cập nhật cột Ảnh khi bạn thả file `NN-ten.png` vào `docs/ui/admin|client`.

## ADMIN

| # | Ảnh | Route | Mục SPEC | API chính (SPEC mục 9) | Quyền | Ghi chú |
|---|---|---|---|---|---|---|
| 01 | ui/admin/01-login.png | /login | 10.14 | POST /auth/login | Public | CHƯA CÓ ẢNH |
| 02 | ui/admin/02-dashboard.png | / | 10.2 | GET /reports/dashboard | STAFF+ | CHƯA CÓ ẢNH — KPI + charts |
| 03 | ui/admin/03-borrow-create-counter.png | /borrows/create | 7.5, 10.3 | GET /readers/lookup, POST /borrows/counter | STAFF+ | Layout 2 cột, quét barcode |
| 04 | ui/admin/04-return-book.png | /returns | 7.7, 10.4 | GET /returns/preview, POST /returns | STAFF+ | |
| 05 | ui/admin/05-borrow-online-requests.png | /borrows/online | 7.6, 10.5 | GET /borrows, POST approve/reject/handover | STAFF+ | Badge chờ duyệt |
| 06 | ui/admin/06-borrow-list.png | /borrows | 7.10, 10.6 | GET /borrows | STAFF+ | |
| 07 | ui/admin/07-borrow-detail.png | /borrows/:id | 7.10, 10.6 | GET /borrows/:id | STAFF+ | Timeline + items |
| 08 | ui/admin/08-overdue.png | /borrows/overdue | 10.7 | GET /borrows/overdue | STAFF+ | |
| 09 | ui/admin/09-reservations.png | /reservations | 10.8, BR-20→24 | GET/POST /reservations | STAFF+ | |
| 10 | ui/admin/10-readers-list.png | /readers | 10.9, M3 | GET /readers | STAFF+ | |
| 11 | ui/admin/11-reader-detail.png | /readers/:id | 10.9, M3 | GET /readers/:id | STAFF+ | Tabs |
| 12 | ui/admin/12-books-list.png | /books | 10.10, M5 | GET /books | STAFF+ | Tổng/có sẵn |
| 13 | ui/admin/13-book-form.png | /books/new\|:id/edit | 10.10, M5–M6 | POST /books, POST copies | STAFF+ | Kèm số lượng bản sao |
| 14 | ui/admin/14-copies.png | /copies | 10.10, M6 | GET /copies, lookup | STAFF+ | |
| 15 | ui/admin/15-fines.png | /fines | 10.11, M11 | GET /fines, POST /payments | STAFF+ | |
| 16 | ui/admin/16-reports.png | /reports | 10.12 | GET /reports/* | STAFF+/ADMIN | |
| 17 | ui/admin/17-settings.png | /settings | 10.13 | GET/PUT /settings | ADMIN | |

## CLIENT

| # | Ảnh | Route | Mục SPEC | API | Quyền | Ghi chú |
|---|---|---|---|---|---|---|
| 01 | ui/client/01-home-a.png … 01-home-d.png | / | 11.2 | GET /books/new, /popular, /news, /categories | Public | Mockup 4 phần trang chủ |
| 01b | — | /news | 11.2 | GET /news/public | Public | Tin tức & hoạt động |
| 01c | — | /sach-moi | 11.2 | GET /books/new | Public | Sách mới (menu) |
| 01d | — | /rules | 11.2 | Trang tĩnh | Public | Quy định mượn trả |
| 01e | — | /about | 11.2 | Trang tĩnh | Public | Giới thiệu |
| 01f | — | /contact | 11.2 | Trang tĩnh | Public | Liên hệ |
| 02 | ui/client/02-book-search-a.png … b.png | /books | 11.2 | GET /books, /categories | Public | Sidebar lọc + thẻ sách + float giỏ |
| 03 | ui/client/03-book-detail.png | /books/:id | 11.2 | GET /books/:id, /copies, /related, /reviews | Public | Giỏ mượn / Đặt trước / bản sao |
| 04 | ui/client/04-login.png | /login | 11.2 | POST /auth/login | Guest | |
| 05 | ui/client/05-register.png | /register | 11.2 | POST /auth/register | Guest | |
| 06 | ui/client/06-borrow-cart-a.png … b.png | /borrow-cart | 7.6 | POST /borrows/online | USER | 3 bước: giỏ → hẹn nhận → mã phiếu |
| 07 | ui/client/07-my-borrows.png | /my/borrows | 11.2 | GET /borrows/me | USER | |
| 08 | ui/client/08-my-reservations.png | /my/reservations | 11.2 | GET /reservations/me | USER | |
| 09 | ui/client/09-my-fines.png | /my/fines | 11.2 | GET /fines/me | USER | |
| 09b | ui/client/09-favorites.png | /me/favorites | 11.2 | GET /favorites/me, /reviews/me | USER | Wishlist + đánh giá |
| 10 | ui/client/10-my-dashboard.png | /my/dashboard | 11.2 | GET /auth/me + stats | USER | |
