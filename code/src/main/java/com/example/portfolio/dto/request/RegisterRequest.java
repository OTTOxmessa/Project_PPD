package com.example.portfolio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email(message = "รูปแบบอีเมลไม่ถูกต้อง") String email,
        @NotBlank @Size(min = 8, message = "รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร") String password
) {
}
