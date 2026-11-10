package com.slms.dto;

/**
 * Lớp bao bọc (wrapper) kết quả trả về chung cho ứng dụng.
 * Cung cấp cấu trúc phản hồi thống nhất với: success, message, data.
 */
public class ApiResponse {

    /** Kết quả thành công hay thất bại */
    private boolean success;

    /** Thông báo mô tả kết quả */
    private String message;

    /** Dữ liệu trả về (có thể là bất kỳ đối tượng nào) */
    private Object data;

    /**
     * Constructor đầy đủ tham số.
     *
     * @param success thành công hay không
     * @param message thông báo
     * @param data    dữ liệu đính kèm
     */
    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // ======================== Factory Methods ========================

    /**
     * Tạo phản hồi thành công với dữ liệu.
     *
     * @param data dữ liệu trả về
     * @return ApiResponse thành công
     */
    public static ApiResponse ok(Object data) {
        return new ApiResponse(true, "Thành công", data);
    }

    /**
     * Tạo phản hồi thành công với thông báo và dữ liệu tùy chỉnh.
     *
     * @param message thông báo
     * @param data    dữ liệu trả về
     * @return ApiResponse thành công
     */
    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    /**
     * Tạo phản hồi lỗi với thông báo.
     *
     * @param message thông báo lỗi
     * @return ApiResponse lỗi
     */
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, null);
    }

    // ======================== Getters & Setters ========================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    // ======================== toString ========================

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
