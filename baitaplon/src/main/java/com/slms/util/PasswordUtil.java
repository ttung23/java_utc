package com.slms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tiện ích mã hóa và kiểm tra mật khẩu sử dụng chuẩn SHA-256 có Salt (Tích hợp sẵn trong JDK).
 * Không phụ thuộc vào thư viện bên ngoài, chạy được trên mọi môi trường Java.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /**
     * Băm mật khẩu sử dụng thuật toán SHA-256 với Salt ngẫu nhiên.
     * Định dạng đầu ra: <Salt_Base64>$<Hash_Base64>
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        byte[] hash = computeHash(plainPassword, salt);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        return saltB64 + "$" + hashB64;
    }

    /**
     * Kiểm tra mật khẩu người dùng nhập vào có khớp với hash đã lưu hay không.
     */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // Trường hợp mật khẩu lưu dạng plaintext (nếu có dữ liệu cũ)
        if (storedHash.equals(plainPassword)) {
            return true;
        }

        int separatorIdx = storedHash.indexOf('$');
        if (separatorIdx <= 0) {
            return false;
        }

        try {
            String saltB64 = storedHash.substring(0, separatorIdx);
            String hashB64 = storedHash.substring(separatorIdx + 1);

            byte[] salt = Base64.getDecoder().decode(saltB64);
            byte[] expectedHash = Base64.getDecoder().decode(hashB64);

            byte[] actualHash = computeHash(plainPassword, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] computeHash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán SHA-256 trong Java Runtime", e);
        }
    }
}
