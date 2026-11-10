package com.slms.model.enums;

/**
 * Trạng thái của bản sao sách trong thư viện.
 * Tương ứng với ràng buộc CHECK trên cột status bảng BOOK_COPIES.
 */
public enum CopyStatus {

    /** Sẵn sàng cho mượn */
    Available,

    /** Đang được mượn */
    Loaned,

    /** Đã được đặt trước */
    Reserved,

    /** Bị hư hỏng */
    Damaged,

    /** Bị mất */
    Lost
}
