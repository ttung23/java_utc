package com.slms.model;

import com.slms.model.enums.ReaderType;

import java.time.LocalDate;

/**
 * Lớp thực thể đại diện cho bạn đọc trong thư viện.
 * Tương ứng với bảng READERS trong cơ sở dữ liệu.
 */
public class Reader {

    /** Mã bạn đọc - khóa chính */
    private String readerId;

    /** Họ và tên đầy đủ */
    private String fullName;

    /** Loại bạn đọc (STUDENT / LECTURER) */
    private ReaderType type;

    /** Ngày hết hạn thẻ thư viện */
    private LocalDate expiryDate;

    /** Số tiền phạt còn nợ (VND) */
    private double balanceFine;

    /** Trạng thái tài khoản: ACTIVE, EXPIRED, BLOCKED */
    private String status;

    /**
     * Constructor mặc định.
     */
    public Reader() {
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param readerId    mã bạn đọc
     * @param fullName    họ và tên
     * @param type        loại bạn đọc
     * @param expiryDate  ngày hết hạn thẻ
     * @param balanceFine số tiền phạt còn nợ
     * @param status      trạng thái tài khoản
     */
    public Reader(String readerId, String fullName, ReaderType type,
                  LocalDate expiryDate, double balanceFine, String status) {
        this.readerId = readerId;
        this.fullName = fullName;
        this.type = type;
        this.expiryDate = expiryDate;
        this.balanceFine = balanceFine;
        this.status = status;
    }

    // ======================== Phương thức nghiệp vụ ========================

    /**
     * Kiểm tra bạn đọc có đủ điều kiện mượn sách hay không.
     * Điều kiện để KHÔNG được mượn:
     * - Trạng thái không phải ACTIVE
     * - Thẻ đã hết hạn (expiryDate < ngày hiện tại)
     * - Tiền phạt vượt quá 50.000 VND
     * - Số sách đang mượn đã đạt giới hạn tối đa
     *
     * @param currentBorrowedCount số sách đang mượn hiện tại
     * @return true nếu bạn đọc đủ điều kiện mượn thêm sách
     */
    public boolean canBorrow(int currentBorrowedCount) {
        // Kiểm tra trạng thái tài khoản
        if (!"ACTIVE".equalsIgnoreCase(this.status)) {
            return false;
        }

        // Kiểm tra thẻ còn hạn
        if (this.expiryDate.isBefore(LocalDate.now())) {
            return false;
        }

        // Kiểm tra tiền phạt không vượt quá ngưỡng cho phép
        if (this.balanceFine > 50000) {
            return false;
        }

        // Kiểm tra chưa đạt giới hạn mượn tối đa
        if (currentBorrowedCount >= this.type.getMaxBooks()) {
            return false;
        }

        return true;
    }

    // ======================== Getters & Setters ========================

    public String getReaderId() {
        return readerId;
    }

    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public ReaderType getType() {
        return type;
    }

    public void setType(ReaderType type) {
        this.type = type;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getBalanceFine() {
        return balanceFine;
    }

    public void setBalanceFine(double balanceFine) {
        this.balanceFine = balanceFine;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "Reader{" +
                "readerId='" + readerId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", type=" + type +
                ", expiryDate=" + expiryDate +
                ", balanceFine=" + balanceFine +
                ", status='" + status + '\'' +
                '}';
    }
}
