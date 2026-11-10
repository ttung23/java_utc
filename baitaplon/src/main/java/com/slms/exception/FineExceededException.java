package com.slms.exception;

/**
 * Ngoại lệ khi tiền phạt còn nợ vượt quá ngưỡng cho phép.
 * Ngưỡng mặc định: 50.000 VND.
 */
public class FineExceededException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo chi tiết.
     *
     * @param message thông báo tiền phạt vượt ngưỡng
     */
    public FineExceededException(String message) {
        super(message);
    }
}
