package com.example.portfolio.exception;

import java.time.LocalDateTime;
import java.util.List;

// รูปแบบ error มาตรฐานของทุก endpoint (ใบงานข้อ 7) — GlobalExceptionHandler และ RestAuthenticationEntryPoint ใช้ร่วมกัน
// ตัวอย่าง:
// {"timestamp":"2026-10-06T18:00:00","status":404,"error":"Not Found",
//  "message":"Portfolio not found: 5","path":"/api/v1/portfolios/5","details":[]}
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
