package com.slms.payment;

/**
 * Cấu hình VNPay.
 *
 * <p>Hai chế độ:</p>
 * <ul>
 *   <li>{@code sandbox} (mặc định) — gọi cổng thật
 *       {@code https://sandbox.vnpayment.vn/paymentv2/vpcpay.html} (cần TmnCode/HashSecret)</li>
 *   <li>{@code local} — giả lập cổng VNPay trên máy, dùng khi chưa có credential</li>
 * </ul>
 *
 * <p>Biến môi trường / system property:</p>
 * <ul>
 *   <li>{@code VNPAY_MODE} / {@code vnpay.mode} = local | sandbox</li>
 *   <li>{@code VNPAY_TMN_CODE} / {@code vnpay.tmnCode}</li>
 *   <li>{@code VNPAY_HASH_SECRET} / {@code vnpay.hashSecret}</li>
 *   <li>{@code VNPAY_PAY_URL} / {@code vnpay.payUrl}</li>
 *   <li>{@code VNPAY_RETURN_URL} / {@code vnpay.returnUrl}</li>
 * </ul>
 *
 * <p>Đăng ký Sandbox (nếu HTTPS lỗi thử HTTP):
 * <a href="http://sandbox.vnpayment.vn/devreg/">http://sandbox.vnpayment.vn/devreg/</a></p>
 */
public final class VnPayConfig {

    public static final String VERSION = "2.1.0";
    public static final String COMMAND = "pay";
    public static final String CURR_CODE = "VND";
    public static final String LOCALE_VN = "vn";
    public static final String ORDER_TYPE = "other";
    public static final int EXPIRE_MINUTES = 15;

    private VnPayConfig() {}

    /** {@code local} hoặc {@code sandbox}. */
    public static String mode() {
        String mode = firstNonBlank(
                System.getenv("VNPAY_MODE"),
                System.getProperty("vnpay.mode"),
                "sandbox"
        );
        return mode == null ? "sandbox" : mode.trim().toLowerCase();
    }

    public static boolean isLocalMode() {
        return "local".equals(mode());
    }

    public static String tmnCode() {
        return firstNonBlank(
                System.getenv("VNPAY_TMN_CODE"),
                System.getProperty("vnpay.tmnCode"),
                "T62CCVC6"
        );
    }

    public static String hashSecret() {
        return firstNonBlank(
                System.getenv("VNPAY_HASH_SECRET"),
                System.getProperty("vnpay.hashSecret"),
                "REGMXLHAXSNWXANVGKGBWZTOOEDSOVXX"
        );
    }

    public static String payUrl() {
        return firstNonBlank(
                System.getenv("VNPAY_PAY_URL"),
                System.getProperty("vnpay.payUrl"),
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        );
    }

    public static String returnUrl(int serverPort) {
        String configured = firstNonBlank(
                System.getenv("VNPAY_RETURN_URL"),
                System.getProperty("vnpay.returnUrl"),
                null
        );
        if (configured != null) {
            return configured;
        }
        return "https://localhost:" + serverPort + "/api/vnpay/return";
    }

    public static String localPayPage(int serverPort) {
        return "https://localhost:" + serverPort + "/client/vnpay-sandbox.html";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
