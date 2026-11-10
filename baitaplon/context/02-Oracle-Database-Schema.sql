-- ====================================================================
-- ORACLE DATABASE DDL SCRIPT FOR SMART LIBRARY MANAGEMENT SYSTEM
-- Compatible with Oracle Database 11g/12c/18c/19c/21c
-- ====================================================================

-- 1. BẢNG DÀNH CHO ĐẦU SÁCH (BOOKS)
CREATE TABLE BOOKS (
    isbn          VARCHAR2(20) NOT NULL,
    title         VARCHAR2(255) NOT NULL,
    author        VARCHAR2(150) NOT NULL,
    publisher     VARCHAR2(150),
    publish_year  NUMBER(4),
    category      VARCHAR2(100),
    CONSTRAINT pk_books PRIMARY KEY (isbn)
);

-- 2. BẢNG DÀNH CHO BẢN SAO SÁCH VẬT LÝ (BOOK_COPIES)
CREATE TABLE BOOK_COPIES (
    copy_id          VARCHAR2(20) NOT NULL,
    isbn             VARCHAR2(20) NOT NULL,
    barcode          VARCHAR2(50) NOT NULL UNIQUE,
    acquisition_date DATE DEFAULT SYSDATE,
    price            NUMBER(12,2) DEFAULT 0,
    status           VARCHAR2(20) DEFAULT 'Available',
    CONSTRAINT pk_book_copies PRIMARY KEY (copy_id),
    CONSTRAINT fk_copies_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn),
    CONSTRAINT chk_copy_status CHECK (status IN ('Available', 'Loaned', 'Reserved', 'Damaged', 'Lost'))
);

-- 3. BẢNG ĐỘC GIẢ (READERS)
CREATE TABLE READERS (
    reader_id    VARCHAR2(20) NOT NULL,
    full_name    VARCHAR2(100) NOT NULL,
    reader_type  VARCHAR2(20) DEFAULT 'STUDENT',
    expiry_date  DATE NOT NULL,
    balance_fine NUMBER(12,2) DEFAULT 0,
    status       VARCHAR2(20) DEFAULT 'ACTIVE',
    CONSTRAINT pk_readers PRIMARY KEY (reader_id),
    CONSTRAINT chk_reader_fine CHECK (balance_fine >= 0),
    CONSTRAINT chk_reader_type CHECK (reader_type IN ('STUDENT', 'LECTURER')),
    CONSTRAINT chk_reader_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'BLOCKED'))
);

-- 4. BẢNG PHIẾU MƯỢN (LOANS)
CREATE TABLE LOANS (
    loan_id     VARCHAR2(20) NOT NULL,
    reader_id   VARCHAR2(20) NOT NULL,
    copy_id     VARCHAR2(20) NOT NULL,
    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    due_date    TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    status      VARCHAR2(20) DEFAULT 'BORROWED',
    CONSTRAINT pk_loans PRIMARY KEY (loan_id),
    CONSTRAINT fk_loans_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id),
    CONSTRAINT fk_loans_copies FOREIGN KEY (copy_id) REFERENCES BOOK_COPIES(copy_id),
    CONSTRAINT chk_loan_date CHECK (due_date >= borrow_date),
    CONSTRAINT chk_loan_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'DAMAGED_RETURN', 'LOST_RETURN'))
);

-- 5. BẢNG HÓA ĐƠN PHẠT (FINES)
CREATE TABLE FINES (
    fine_id        VARCHAR2(20) NOT NULL,
    loan_id        VARCHAR2(20) NOT NULL,
    reason         VARCHAR2(255),
    fine_amount    NUMBER(12,2) NOT NULL,
    payment_status VARCHAR2(20) DEFAULT 'Unpaid',
    payment_date   TIMESTAMP,
    CONSTRAINT pk_fines PRIMARY KEY (fine_id),
    CONSTRAINT fk_fines_loans FOREIGN KEY (loan_id) REFERENCES LOANS(loan_id),
    CONSTRAINT chk_fine_amount CHECK (fine_amount >= 0),
    CONSTRAINT chk_fine_payment_status CHECK (payment_status IN ('Unpaid', 'Paid'))
);

-- 6. BẢNG ĐẶT TRƯỚC (RESERVATIONS)
CREATE TABLE RESERVATIONS (
    reservation_id   VARCHAR2(20) NOT NULL,
    reader_id        VARCHAR2(20) NOT NULL,
    isbn             VARCHAR2(20) NOT NULL,
    reservation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expiry_date      TIMESTAMP NOT NULL,
    status           VARCHAR2(20) DEFAULT 'Pending',
    CONSTRAINT pk_reservations PRIMARY KEY (reservation_id),
    CONSTRAINT fk_res_readers FOREIGN KEY (reader_id) REFERENCES READERS(reader_id),
    CONSTRAINT fk_res_books FOREIGN KEY (isbn) REFERENCES BOOKS(isbn),
    CONSTRAINT chk_res_status CHECK (status IN ('Pending', 'Fulfilled', 'Expired', 'Cancelled'))
);

-- INDEXES CHO TỐI ƯU HÓA TRUY VẤN
CREATE INDEX idx_copies_barcode ON BOOK_COPIES(barcode);
CREATE INDEX idx_loans_reader ON LOANS(reader_id);
CREATE INDEX idx_loans_copy ON LOANS(copy_id);
CREATE INDEX idx_fines_loan ON FINES(loan_id);
