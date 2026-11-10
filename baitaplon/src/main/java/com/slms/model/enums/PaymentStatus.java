package com.slms.model.enums;

/**
 * Trạng thái thanh toán phạt.
 * Tương ứng với ràng buộc CHECK trên cột payment_status bảng FINES.
 */
public enum PaymentStatus {

    /** Chưa thanh toán */
    Unpaid,

    /** Đã thanh toán */
    Paid
}
