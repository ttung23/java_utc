package com.slms.exception;

/**
 * Ngoại lệ khi bản sao sách không sẵn sàng cho mượn.
 * Ví dụ: sách đang được mượn, đã đặt trước, bị hư hỏng hoặc mất.
 */
public class BookNotAvailableException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông báo chi tiết.
     *
     * @param message thông báo lý do sách không khả dụng
     */
    public BookNotAvailableException(String message) {
        super(message);
    }
}
