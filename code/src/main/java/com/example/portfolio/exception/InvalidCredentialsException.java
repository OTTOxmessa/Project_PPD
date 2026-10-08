package com.example.portfolio.exception;

// login ไม่สำเร็จ -> 401 Unauthorized
// ใช้ข้อความเดียวกันทั้งกรณี "ไม่พบอีเมล" และ "รหัสผ่านผิด" เพื่อไม่ให้ผู้ไม่หวังดีใช้ทดสอบว่าอีเมลไหนมีบัญชีอยู่
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("อีเมลหรือรหัสผ่านไม่ถูกต้อง");
    }
}
