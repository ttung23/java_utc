package com.slms.model.enums;

/**
 * Trạng thái đặt trước sách.
 * Tương ứng với ràng buộc CHECK trên cột status bảng RESERVATIONS.
 */
public enum ReservationStatus {

    /** Đang chờ xử lý */
    Pending,

    /** Đã hoàn thành (sách đã được nhận) */
    Fulfilled,

    /** Đã hết hạn đặt trước */
    Expired,

    /** Đã bị hủy */
    Cancelled
}
