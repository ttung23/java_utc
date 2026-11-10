package com.slms.dto;

import java.time.LocalDateTime;

/**
 * DTO hiển thị thông tin phiếu mượn kết hợp nhiều bảng.
 * Dùng để truyền dữ liệu giữa tầng service và tầng giao diện.
 */
public class LoanDTO {

    /** Mã phiếu mượn */
    private String loanId;

    /** Mã bạn đọc */
    private String readerId;

    /** Tên bạn đọc (JOIN từ bảng READERS) */
    private String readerName;

    /** Mã bản sao */
    private String copyId;

    /** Mã vạch (JOIN từ bảng BOOK_COPIES) */
    private String barcode;

    /** Tên sách (JOIN từ bảng BOOKS) */
    private String bookTitle;

    /** Ngày giờ mượn */
    private LocalDateTime borrowDate;

    /** Hạn trả */
    private LocalDateTime dueDate;

    /** Trạng thái mượn (dạng chuỗi) */
    private String status;

    /**
     * Constructor mặc định.
     */
    public LoanDTO() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param loanId     mã phiếu mượn
     * @param readerId   mã bạn đọc
     * @param readerName tên bạn đọc
     * @param copyId     mã bản sao
     * @param barcode    mã vạch
     * @param bookTitle  tên sách
     * @param borrowDate ngày mượn
     * @param dueDate    hạn trả
     * @param status     trạng thái
     */
    public LoanDTO(String loanId, String readerId, String readerName,
                   String copyId, String barcode, String bookTitle,
                   LocalDateTime borrowDate, LocalDateTime dueDate, String status) {
        this.loanId = loanId;
        this.readerId = readerId;
        this.readerName = readerName;
        this.copyId = copyId;
        this.barcode = barcode;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    // ======================== Getters & Setters ========================

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
        this.copyId = copyId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "LoanDTO{" +
                "loanId='" + loanId + '\'' +
                ", readerId='" + readerId + '\'' +
                ", readerName='" + readerName + '\'' +
                ", copyId='" + copyId + '\'' +
                ", barcode='" + barcode + '\'' +
                ", bookTitle='" + bookTitle + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", status='" + status + '\'' +
                '}';
    }
}
