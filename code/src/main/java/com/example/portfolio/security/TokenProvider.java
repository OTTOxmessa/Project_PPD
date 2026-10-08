package com.example.portfolio.security;

// สิ่งที่ระบบต้องการจาก token: ออก, ตรวจ, อ่าน userId — ไม่ผูกกับ JWT หรือไลบรารีใดตรง ๆ (DIP)
public interface TokenProvider {

    String generateToken(Long userId, String email);

    boolean isValid(String token);

    Long extractUserId(String token);
}
