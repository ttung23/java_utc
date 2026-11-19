package com.slms.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiện ích ký HMAC-SHA512 và dựng URL thanh toán theo chuẩn VNPay 2.1.0
 * (giống mã mẫu Java của VNPay: URLEncoder kiểu US-ASCII, khoảng trắng = '+').
 */
public final class VnPayUtil {

    private VnPayUtil() {}

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA512: " + e.getMessage(), e);
        }
    }

    /**
     * Xây URL thanh toán từ map tham số (chưa gồm vnp_SecureHash).
     */
    public static String buildPaymentUrl(String baseUrl, Map<String, String> params, String hashSecret) {
        String query = buildQuery(params, hashSecret);
        return baseUrl + "?" + query;
    }

    /**
     * Dựng chuỗi query đã ký (dùng cho Return URL giả lập).
     */
    public static String buildQuery(Map<String, String> params, String hashSecret) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            String encodedValue = urlEncode(fieldValue);
            hashData.append(fieldName).append('=').append(encodedValue);
            query.append(urlEncode(fieldName)).append('=').append(encodedValue);
            if (itr.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        trimTrailingAmpersand(hashData);
        trimTrailingAmpersand(query);

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }

    /**
     * Xác thực chữ ký từ query params VNPay trả về (return/IPN).
     */
    public static boolean validateSecureHash(Map<String, String> fields, String hashSecret) {
        String receivedHash = fields.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> copy = new LinkedHashMap<>(fields);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(copy.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = copy.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            hashData.append(fieldName).append('=').append(urlEncode(fieldValue));
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }
        trimTrailingAmpersand(hashData);

        String calculated = hmacSHA512(hashSecret, hashData.toString());
        return calculated.equalsIgnoreCase(receivedHash);
    }

    /**
     * Tương thích {@code URLEncoder.encode(..., "US-ASCII")} của Java:
     * khoảng trắng thành {@code +}, ký tự đặc biệt thành {@code %XX}.
     */
    public static String urlEncode(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '*') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('+');
            } else {
                byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    sb.append('%');
                    String hex = Integer.toHexString(b & 0xFF).toUpperCase();
                    if (hex.length() == 1) {
                        sb.append('0');
                    }
                    sb.append(hex);
                }
            }
        }
        return sb.toString();
    }

    private static void trimTrailingAmpersand(StringBuilder sb) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '&') {
            sb.setLength(sb.length() - 1);
        }
    }
}
