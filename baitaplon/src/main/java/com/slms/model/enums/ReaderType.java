package com.slms.model.enums;

/**
 * Loại bạn đọc trong hệ thống thư viện.
 * Mỗi loại có giới hạn số sách được mượn và số ngày mượn tối đa.
 */
public enum ReaderType {

    /** Sinh viên: tối đa 5 cuốn, mượn tối đa 14 ngày */
    STUDENT(5, 14),

    /** Giảng viên: tối đa 10 cuốn, mượn tối đa 30 ngày */
    LECTURER(10, 30);

    /** Số sách tối đa được mượn cùng lúc */
    private final int maxBooks;

    /** Số ngày mượn tối đa */
    private final int maxBorrowDays;

    /**
     * Khởi tạo loại bạn đọc với giới hạn mượn.
     *
     * @param maxBooks      số sách tối đa được mượn
     * @param maxBorrowDays số ngày mượn tối đa
     */
    ReaderType(int maxBooks, int maxBorrowDays) {
        this.maxBooks = maxBooks;
        this.maxBorrowDays = maxBorrowDays;
    }

    /**
     * Lấy số sách tối đa được mượn cùng lúc.
     *
     * @return số sách tối đa
     */
    public int getMaxBooks() {
        return maxBooks;
    }

    /**
     * Lấy số ngày mượn tối đa.
     *
     * @return số ngày mượn tối đa
     */
    public int getMaxBorrowDays() {
        return maxBorrowDays;
    }
}
