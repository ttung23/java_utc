-- ============================================================
-- SLMS - Smart Library Management System
-- Script tạo cơ sở dữ liệu Oracle
-- Bao gồm: DDL bảng, ràng buộc, chỉ mục và dữ liệu mẫu
-- ============================================================

-- ============================================================
-- XÓA BẢNG CŨ (nếu tồn tại) - thứ tự ngược theo phụ thuộc
-- ============================================================
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVATIONS CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE FINES CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE LOANS CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE READERS CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE BOOK_COPIES CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE BOOKS CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- ============================================================
-- 1. BẢNG BOOKS - Lưu thông tin sách (theo ISBN)
-- ============================================================
CREATE TABLE BOOKS (
    isbn            VARCHAR2(20)    NOT NULL,
    title           VARCHAR2(255)   NOT NULL,
    author          VARCHAR2(150)   NOT NULL,
    publisher       VARCHAR2(150),
    publish_year    NUMBER(4),
    category        VARCHAR2(100),
    CONSTRAINT pk_books PRIMARY KEY (isbn)
);

-- ============================================================
-- 2. BẢNG BOOK_COPIES - Lưu bản sao cụ thể của sách
-- ============================================================
CREATE TABLE BOOK_COPIES (
    copy_id             VARCHAR2(20)    NOT NULL,
    isbn                VARCHAR2(20)    NOT NULL,
    barcode             VARCHAR2(50)    NOT NULL UNIQUE,
    acquisition_date    DATE            DEFAULT SYSDATE,
    price               NUMBER(12,2)    DEFAULT 0,
    status              VARCHAR2(20)    DEFAULT 'Available',
    CONSTRAINT pk_book_copies PRIMARY KEY (copy_id),
    CONSTRAINT fk_copies_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn),
    CONSTRAINT chk_copy_status CHECK (status IN ('Available', 'Loaned', 'Reserved', 'Damaged', 'Lost'))
);

-- ============================================================
-- 3. BẢNG READERS - Lưu thông tin bạn đọc
-- ============================================================
CREATE TABLE READERS (
    reader_id       VARCHAR2(20)    NOT NULL,
    full_name       VARCHAR2(100)   NOT NULL,
    reader_type     VARCHAR2(20)    DEFAULT 'STUDENT',
    expiry_date     DATE            NOT NULL,
    balance_fine    NUMBER(12,2)    DEFAULT 0,
    status          VARCHAR2(20)    DEFAULT 'ACTIVE',
    CONSTRAINT pk_readers PRIMARY KEY (reader_id),
    CONSTRAINT chk_reader_fine CHECK (balance_fine >= 0),
    CONSTRAINT chk_reader_type CHECK (reader_type IN ('STUDENT', 'LECTURER')),
    CONSTRAINT chk_reader_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'BLOCKED'))
);

-- ============================================================
-- 4. BẢNG LOANS - Lưu thông tin mượn/trả sách
-- ============================================================
CREATE TABLE LOANS (
    loan_id         VARCHAR2(20)    NOT NULL,
    reader_id       VARCHAR2(20)    NOT NULL,
    copy_id         VARCHAR2(20)    NOT NULL,
    borrow_date     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    due_date        TIMESTAMP       NOT NULL,
    return_date     TIMESTAMP,
    status          VARCHAR2(20)    DEFAULT 'BORROWED',
    CONSTRAINT pk_loans PRIMARY KEY (loan_id),
    CONSTRAINT fk_loans_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id),
    CONSTRAINT fk_loans_copies FOREIGN KEY (copy_id) REFERENCES BOOK_COPIES(copy_id),
    CONSTRAINT chk_loan_date CHECK (due_date >= borrow_date),
    CONSTRAINT chk_loan_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'DAMAGED_RETURN', 'LOST_RETURN'))
);

-- ============================================================
-- 5. BẢNG FINES - Lưu thông tin phạt
-- ============================================================
CREATE TABLE FINES (
    fine_id             VARCHAR2(20)    NOT NULL,
    loan_id             VARCHAR2(20)    NOT NULL,
    reason              VARCHAR2(255),
    fine_amount         NUMBER(12,2)    NOT NULL,
    payment_status      VARCHAR2(20)    DEFAULT 'Unpaid',
    payment_date        TIMESTAMP,
    CONSTRAINT pk_fines PRIMARY KEY (fine_id),
    CONSTRAINT fk_fines_loans FOREIGN KEY (loan_id) REFERENCES LOANS(loan_id),
    CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
    CONSTRAINT chk_fine_payment_status CHECK (payment_status IN ('Unpaid', 'Paid'))
);

-- ============================================================
-- 6. BẢNG RESERVATIONS - Lưu thông tin đặt trước sách
-- ============================================================
CREATE TABLE RESERVATIONS (
    reservation_id      VARCHAR2(20)    NOT NULL,
    reader_id           VARCHAR2(20)    NOT NULL,
    isbn                VARCHAR2(20)    NOT NULL,
    reservation_date    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_date         TIMESTAMP       NOT NULL,
    status              VARCHAR2(20)    DEFAULT 'Pending',
    CONSTRAINT pk_reservations PRIMARY KEY (reservation_id),
    CONSTRAINT fk_res_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id),
    CONSTRAINT fk_res_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn),
    CONSTRAINT chk_res_status CHECK (status IN ('Pending', 'Fulfilled', 'Expired', 'Cancelled'))
);

-- ============================================================
-- CHỈ MỤC (Indexes) - Tối ưu hiệu suất truy vấn
-- ============================================================
CREATE INDEX idx_copies_barcode ON BOOK_COPIES(barcode);
CREATE INDEX idx_loans_reader   ON LOANS(reader_id);
CREATE INDEX idx_loans_copy     ON LOANS(copy_id);
CREATE INDEX idx_fines_loan     ON FINES(loan_id);

-- ============================================================
-- DỮ LIỆU MẪU - 10 cuốn sách
-- ============================================================
INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-13-468599-1', 'The Pragmatic Programmer', 'David Thomas, Andrew Hunt', 'Addison-Wesley', 2019, 'Software Engineering');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-596-51774-8', 'JavaScript: The Good Parts', 'Douglas Crockford', 'O''Reilly Media', 2008, 'Programming');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-13-235088-4', 'Clean Code', 'Robert C. Martin', 'Prentice Hall', 2008, 'Software Engineering');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-201-63361-0', 'Design Patterns', 'Erich Gamma, Richard Helm', 'Addison-Wesley', 1994, 'Software Engineering');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-13-468729-2', 'Effective Java', 'Joshua Bloch', 'Addison-Wesley', 2018, 'Java');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-321-12521-7', 'Domain-Driven Design', 'Eric Evans', 'Addison-Wesley', 2003, 'Software Architecture');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-1-49-195016-0', 'Head First Java', 'Kathy Sierra, Bert Bates', 'O''Reilly Media', 2022, 'Java');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-13-411025-2', 'Java Concurrency in Practice', 'Brian Goetz', 'Addison-Wesley', 2006, 'Java');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-1-61-729008-5', 'Spring in Action', 'Craig Walls', 'Manning', 2022, 'Java');

INSERT INTO BOOKS (isbn, title, author, publisher, publish_year, category)
VALUES ('978-0-59-651798-4', 'Learning SQL', 'Alan Beaulieu', 'O''Reilly Media', 2020, 'Database');

-- ============================================================
-- DỮ LIỆU MẪU - 15 bản sao sách
-- ============================================================
INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP001', '978-0-13-468599-1', 'BC-00001', DATE '2023-01-15', 450000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP002', '978-0-13-468599-1', 'BC-00002', DATE '2023-01-15', 450000, 'Loaned');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP003', '978-0-596-51774-8', 'BC-00003', DATE '2023-02-20', 320000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP004', '978-0-13-235088-4', 'BC-00004', DATE '2023-03-10', 380000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP005', '978-0-13-235088-4', 'BC-00005', DATE '2023-03-10', 380000, 'Loaned');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP006', '978-0-201-63361-0', 'BC-00006', DATE '2023-04-05', 520000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP007', '978-0-13-468729-2', 'BC-00007', DATE '2023-05-12', 410000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP008', '978-0-13-468729-2', 'BC-00008', DATE '2023-05-12', 410000, 'Loaned');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP009', '978-0-321-12521-7', 'BC-00009', DATE '2023-06-01', 490000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP010', '978-1-49-195016-0', 'BC-00010', DATE '2023-07-18', 350000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP011', '978-1-49-195016-0', 'BC-00011', DATE '2023-07-18', 350000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP012', '978-0-13-411025-2', 'BC-00012', DATE '2023-08-22', 440000, 'Damaged');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP013', '978-1-61-729008-5', 'BC-00013', DATE '2023-09-05', 470000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP014', '978-1-61-729008-5', 'BC-00014', DATE '2023-09-05', 470000, 'Available');

INSERT INTO BOOK_COPIES (copy_id, isbn, barcode, acquisition_date, price, status)
VALUES ('CP015', '978-0-59-651798-4', 'BC-00015', DATE '2023-10-11', 300000, 'Available');

-- ============================================================
-- DỮ LIỆU MẪU - 5 bạn đọc (3 STUDENT, 2 LECTURER)
-- ============================================================
INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status)
VALUES ('RD001', 'Nguyen Van An', 'STUDENT', DATE '2025-12-31', 0, 'ACTIVE');

INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status)
VALUES ('RD002', 'Tran Thi Binh', 'STUDENT', DATE '2025-12-31', 15000, 'ACTIVE');

INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status)
VALUES ('RD003', 'Le Hoang Cuong', 'STUDENT', DATE '2024-06-30', 0, 'EXPIRED');

INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status)
VALUES ('RD004', 'Pham Minh Duc', 'LECTURER', DATE '2026-12-31', 0, 'ACTIVE');

INSERT INTO READERS (reader_id, full_name, reader_type, expiry_date, balance_fine, status)
VALUES ('RD005', 'Vo Thi Hoa', 'LECTURER', DATE '2026-06-30', 25000, 'ACTIVE');

-- ============================================================
-- DỮ LIỆU MẪU - Phiếu mượn
-- ============================================================
INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status)
VALUES ('LN001', 'RD001', 'CP002', TIMESTAMP '2024-11-01 09:30:00', TIMESTAMP '2024-11-15 09:30:00', NULL, 'BORROWED');

INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status)
VALUES ('LN002', 'RD002', 'CP005', TIMESTAMP '2024-10-20 14:00:00', TIMESTAMP '2024-11-03 14:00:00', NULL, 'OVERDUE');

INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status)
VALUES ('LN003', 'RD004', 'CP008', TIMESTAMP '2024-11-05 10:15:00', TIMESTAMP '2024-12-05 10:15:00', NULL, 'BORROWED');

INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status)
VALUES ('LN004', 'RD001', 'CP004', TIMESTAMP '2024-09-01 08:00:00', TIMESTAMP '2024-09-15 08:00:00', TIMESTAMP '2024-09-14 16:30:00', 'RETURNED');

INSERT INTO LOANS (loan_id, reader_id, copy_id, borrow_date, due_date, return_date, status)
VALUES ('LN005', 'RD005', 'CP006', TIMESTAMP '2024-10-10 11:00:00', TIMESTAMP '2024-11-09 11:00:00', TIMESTAMP '2024-11-12 09:00:00', 'RETURNED');

-- ============================================================
-- DỮ LIỆU MẪU - Phạt
-- ============================================================
INSERT INTO FINES (fine_id, loan_id, reason, fine_amount, payment_status, payment_date)
VALUES ('FN001', 'LN002', 'Trả sách quá hạn', 15000, 'Unpaid', NULL);

INSERT INTO FINES (fine_id, loan_id, reason, fine_amount, payment_status, payment_date)
VALUES ('FN002', 'LN005', 'Trả sách quá hạn 3 ngày', 15000, 'Paid', TIMESTAMP '2024-11-12 09:05:00');

COMMIT;
