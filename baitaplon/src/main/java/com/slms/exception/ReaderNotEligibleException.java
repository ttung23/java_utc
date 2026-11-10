package com.slms.exception;

/**
 * Ngoại lệ khi bạn đọc không đủ điều kiện mượn sách.
 * Ví dụ: thẻ hết hạn, tài khoản bị khóa, nợ phạt quá nhiều.
 */
public class ReaderNotEligibleException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo chi tiết.
     *
     * @param message thông báo lý do không đủ điều kiện
     */
    public ReaderNotEligibleException(String message) {
        super(message);
    }
}
