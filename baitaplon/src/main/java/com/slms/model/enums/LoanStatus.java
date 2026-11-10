package com.slms.model.enums;

/**
 * Trạng thái của phiếu mượn sách.
 * Tương ứng với ràng buộc CHECK trên cột status bảng LOANS.
 */
public enum LoanStatus {

    /** Đang mượn */
    BORROWED,

    /** Đã trả */
    RETURNED,

    /** Quá hạn */
    OVERDUE,

    /** Trả sách bị hư hỏng */
    DAMAGED_RETURN,

    /** Khai báo mất sách */
    LOST_RETURN
}
