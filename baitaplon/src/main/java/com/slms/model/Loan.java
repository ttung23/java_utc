package com.slms.model;

import com.slms.model.enums.LoanStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Lớp thực thể đại diện cho phiếu mượn sách.
 * Tương ứng với bảng LOANS trong cơ sở dữ liệu.
 */
public class Loan {

    /** Mã phiếu mượn - khóa chính */
    private String loanId;

    /** Mã bạn đọc */
    private String readerId;

    /** Mã bản sao sách */
    private String copyId;

    /** Ngày giờ mượn */
    private LocalDateTime borrowDate;

    /** Ngày giờ phải trả (hạn trả) */
    private LocalDateTime dueDate;

    /** Ngày giờ thực tế trả sách (null nếu chưa trả) */
    private LocalDateTime returnDate;

    /** Trạng thái phiếu mượn */
    private LoanStatus status;

    /**
     * Constructor mặc định.
     */
    public Loan() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param loanId     mã phiếu mượn
     * @param readerId   mã bạn đọc
     * @param copyId     mã bản sao
     * @param borrowDate ngày mượn
     * @param dueDate    hạn trả
     * @param returnDate ngày trả thực tế
     * @param status     trạng thái
     */
    public Loan(String loanId, String readerId, String copyId,
                LocalDateTime borrowDate, LocalDateTime dueDate,
                LocalDateTime returnDate, LoanStatus status) {
        this.loanId = loanId;
        this.readerId = readerId;
        this.copyId = copyId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // ======================== Phương thức nghiệp vụ ========================

    /**
     * Tính số ngày quá hạn trả sách.
     * Nếu đã trả sách → so sánh returnDate với dueDate.
     * Nếu chưa trả → so sánh thời điểm hiện tại với dueDate.
     *
     * @return số ngày quá hạn (0 nếu chưa quá hạn)
     */
    public long calculateOverdueDays() {
        LocalDateTime referenceDate = (this.returnDate != null) ? this.returnDate : LocalDateTime.now();

        long overdueDays = ChronoUnit.DAYS.between(this.dueDate, referenceDate);

        // Chỉ trả về số dương khi thực sự quá hạn
        return Math.max(0, overdueDays);
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

    public String getCopyId() {
        return copyId;
    }

    public void setCopyId(String copyId) {
        this.copyId = copyId;
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

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "Loan{" +
                "loanId='" + loanId + '\'' +
                ", readerId='" + readerId + '\'' +
                ", copyId='" + copyId + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                ", status=" + status +
                '}';
    }
}
