package com.slms.dto;

import java.time.LocalDateTime;

/**
 * DTO chứa kết quả xử lý trả sách.
 * Bao gồm thông tin phạt (nếu có) và trạng thái mới của bản sao.
 */
public class ReturnResultDTO {

    /** Mã phiếu mượn */
    private String loanId;

    /** Mã bản sao */
    private String copyId;

    /** Mã vạch */
    private String barcode;

    /** Ngày giờ trả sách */
    private LocalDateTime returnDate;

    /** Số ngày trả trễ */
    private long overdueDays;

    /** Số tiền phạt (VND) */
    private double fineAmount;

    /** Trạng thái mới của bản sao sau khi trả */
    private String copyNewStatus;

    /** Thông báo kết quả trả sách */
    private String message;

    /**
     * Constructor mặc định.
     */
    public ReturnResultDTO() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param loanId        mã phiếu mượn
     * @param copyId        mã bản sao
     * @param barcode       mã vạch
     * @param returnDate    ngày trả
     * @param overdueDays   số ngày trễ
     * @param fineAmount    số tiền phạt
     * @param copyNewStatus trạng thái mới của bản sao
     * @param message       thông báo kết quả
     */
    public ReturnResultDTO(String loanId, String copyId, String barcode,
                           LocalDateTime returnDate, long overdueDays,
                           double fineAmount, String copyNewStatus, String message) {
        this.loanId = loanId;
        this.copyId = copyId;
        this.barcode = barcode;
        this.returnDate = returnDate;
        this.overdueDays = overdueDays;
        this.fineAmount = fineAmount;
        this.copyNewStatus = copyNewStatus;
        this.message = message;
    }

    // ======================== Getters & Setters ========================

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
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

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        this.overdueDays = overdueDays;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getCopyNewStatus() {
        return copyNewStatus;
    }

    public void setCopyNewStatus(String copyNewStatus) {
        this.copyNewStatus = copyNewStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "ReturnResultDTO{" +
                "loanId='" + loanId + '\'' +
                ", copyId='" + copyId + '\'' +
                ", barcode='" + barcode + '\'' +
                ", returnDate=" + returnDate +
                ", overdueDays=" + overdueDays +
                ", fineAmount=" + fineAmount +
                ", copyNewStatus='" + copyNewStatus + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
