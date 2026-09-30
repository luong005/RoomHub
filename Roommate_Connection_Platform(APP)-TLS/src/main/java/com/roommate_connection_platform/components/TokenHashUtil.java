package com.roommate_connection_platform.components;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class TokenHashUtil {

    /**
     * Hàm băm một chuỗi bất kỳ bằng thuật toán SHA-256
     * Sau đó chuyển kết quả nhị phân sang định dạng Base64
     */
    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            throw new IllegalArgumentException("Token không được để trống");
        }

        try {
            // 1. Khai báo sử dụng thuật toán SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 2. Băm toàn bộ chuỗi (không bị giới hạn 72 bytes)
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            // 3. Đóng gói mảng byte thành chuỗi Base64 gọn gàng để lưu DB
            return Base64.getEncoder().encodeToString(encodedHash);

        } catch (NoSuchAlgorithmException e) {
            // Lỗi này thực tế rất khó xảy ra vì SHA-256 là chuẩn mặc định của Java
            throw new RuntimeException("Hệ thống không hỗ trợ thuật toán SHA-256", e);
        }
    }
}
