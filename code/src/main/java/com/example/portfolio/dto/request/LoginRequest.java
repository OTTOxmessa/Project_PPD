package com.example.portfolio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "รูปแบบอีเมลไม่ถูกต้อง") String email,
        @NotBlank String password
) {
}
