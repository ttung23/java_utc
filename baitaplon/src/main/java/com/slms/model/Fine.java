package com.slms.model;

import com.slms.model.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Lớp thực thể đại diện cho khoản phạt.
 * Tương ứng với bảng FINES trong cơ sở dữ liệu.
 */
public class Fine {

    /** Đơn giá phạt quá hạn mỗi ngày (VND) */
    private static final double OVERDUE_FINE_PER_DAY = 5000.0;

    /** Mã phạt - khóa chính */
    private String fineId;

    /** Mã phiếu mượn liên kết */
    private String loanId;

    /** Lý do phạt */
    private String reason;

    /** Số tiền phạt (VND) */
    private double fineAmount;

    /** Trạng thái thanh toán */
    private PaymentStatus paymentStatus;

    /** Ngày giờ thanh toán (null nếu chưa thanh toán) */
    private LocalDateTime paymentDate;

    /**
     * Constructor mặc định.
     */
    public Fine() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param fineId        mã phạt
     * @param loanId        mã phiếu mượn
     * @param reason        lý do phạt
     * @param fineAmount    số tiền phạt
     * @param paymentStatus trạng thái thanh toán
     * @param paymentDate   ngày thanh toán
     */
    public Fine(String fineId, String loanId, String reason, double fineAmount,
                PaymentStatus paymentStatus, LocalDateTime paymentDate) {
        this.fineId = fineId;
        this.loanId = loanId;
        this.reason = reason;
        this.fineAmount = fineAmount;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    // ======================== Phương thức tĩnh ========================

    /**
     * Tính tiền phạt quá hạn dựa trên số ngày trễ.
     * Công thức: overdueDays * 5000 VND/ngày.
     *
     * @param overdueDays số ngày quá hạn
     * @return số tiền phạt (VND)
     */
    public static double CALCULATE_OVERDUE_FINE(long overdueDays) {
        return overdueDays * OVERDUE_FINE_PER_DAY;
    }

    // ======================== Getters & Setters ========================

    public String getFineId() {
        return fineId;
    }

    public void setFineId(String fineId) {
        this.fineId = fineId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "Fine{" +
                "fineId='" + fineId + '\'' +
                ", loanId='" + loanId + '\'' +
                ", reason='" + reason + '\'' +
                ", fineAmount=" + fineAmount +
                ", paymentStatus=" + paymentStatus +
                ", paymentDate=" + paymentDate +
                '}';
    }
}
