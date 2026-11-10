package com.slms.util;

import java.util.Random;

/**
 * Lớp tiện ích tạo mã ID duy nhất cho các thực thể.
 * Sử dụng prefix + timestamp + 3 chữ số ngẫu nhiên để đảm bảo tính duy nhất.
 */
public final class IdGenerator {

    /** Bộ sinh số ngẫu nhiên */
    private static final Random RANDOM = new Random();

    /**
     * Constructor private - không cho phép tạo instance.
     */
    private IdGenerator() {
        throw new UnsupportedOperationException("Lớp tiện ích - không được tạo instance");
    }

    /**
     * Sinh mã phiếu mượn.
     * Định dạng: LN + timestamp + 3 chữ số ngẫu nhiên.
     *
     * @return mã phiếu mượn duy nhất
     */
    public static String generateLoanId() {
        return generateId("LN");
    }

    /**
     * Sinh mã phạt.
     * Định dạng: FN + timestamp + 3 chữ số ngẫu nhiên.
     *
     * @return mã phạt duy nhất
     */
    public static String generateFineId() {
        return generateId("FN");
    }

    /**
     * Sinh mã đặt trước.
     * Định dạng: RS + timestamp + 3 chữ số ngẫu nhiên.
     *
     * @return mã đặt trước duy nhất
     */
    public static String generateReservationId() {
        return generateId("RS");
    }

    /**
     * Phương thức nội bộ sinh mã ID với prefix tùy chỉnh.
     *
     * @param prefix tiền tố mã ID
     * @return mã ID duy nhất
     */
    private static String generateId(String prefix) {
        long timestamp = System.currentTimeMillis();
        int randomSuffix = RANDOM.nextInt(900) + 100; // 100-999 (3 chữ số)
        return prefix + timestamp + randomSuffix;
    }
}
