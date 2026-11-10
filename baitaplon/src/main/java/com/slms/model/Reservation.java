package com.slms.model;

import com.slms.model.enums.ReservationStatus;

import java.time.LocalDateTime;

/**
 * Lớp thực thể đại diện cho phiếu đặt trước sách.
 * Tương ứng với bảng RESERVATIONS trong cơ sở dữ liệu.
 */
public class Reservation {

    /** Mã đặt trước - khóa chính */
    private String reservationId;

    /** Mã bạn đọc */
    private String readerId;

    /** Mã ISBN của sách được đặt */
    private String isbn;

    /** Ngày giờ đặt trước */
    private LocalDateTime reservationDate;

    /** Ngày giờ hết hạn đặt trước */
    private LocalDateTime expiryDate;

    /** Trạng thái đặt trước */
    private ReservationStatus status;

    /**
     * Constructor mặc định.
     */
    public Reservation() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param reservationId   mã đặt trước
     * @param readerId        mã bạn đọc
     * @param isbn            mã ISBN
     * @param reservationDate ngày đặt
     * @param expiryDate      ngày hết hạn
     * @param status          trạng thái
     */
    public Reservation(String reservationId, String readerId, String isbn,
                       LocalDateTime reservationDate, LocalDateTime expiryDate,
                       ReservationStatus status) {
        this.reservationId = reservationId;
        this.readerId = readerId;
        this.isbn = isbn;
        this.reservationDate = reservationDate;
        this.expiryDate = expiryDate;
        this.status = status;
    }

    // ======================== Getters & Setters ========================

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", readerId='" + readerId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", reservationDate=" + reservationDate +
                ", expiryDate=" + expiryDate +
                ", status=" + status +
                '}';
    }
}
