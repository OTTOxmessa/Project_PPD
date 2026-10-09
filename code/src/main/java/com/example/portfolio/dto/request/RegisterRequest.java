package com.example.portfolio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "กรุณากรอกชื่อผู้ใช้") String username,
        @NotBlank(message = "กรุณากรอกอีเมล") @Email(message = "รูปแบบอีเมลไม่ถูกต้อง") String email,
        @NotBlank(message = "กรุณากรอกรหัสผ่าน") @Size(min = 8, message = "รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร") String password
) {
}
