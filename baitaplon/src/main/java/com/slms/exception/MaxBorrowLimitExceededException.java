package com.slms.exception;

/**
 * Ngoại lệ khi bạn đọc đã mượn đủ số sách tối đa cho phép.
 * Sinh viên tối đa 5 cuốn, Giảng viên tối đa 10 cuốn.
 */
public class MaxBorrowLimitExceededException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo chi tiết.
     *
     * @param message thông báo vượt quá giới hạn mượn
     */
    public MaxBorrowLimitExceededException(String message) {
        super(message);
    }
}
