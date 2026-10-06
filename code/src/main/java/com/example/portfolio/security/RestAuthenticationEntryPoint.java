package com.example.portfolio.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// ไม่มี token / token หมดอายุ -> 401 ในรูปแบบเดียวกับ ErrorResponse ของ GlobalExceptionHandler
// (Spring Security ทำงานก่อนถึง controller จึงต้องเขียน JSON เอง @RestControllerAdvice มองไม่เห็น error นี้)
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MESSAGE = "กรุณาเข้าสู่ระบบ (ไม่มี token หรือ token หมดอายุ)";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + status.getReasonPhrase() + "\","
                + "\"message\":\"" + MESSAGE + "\","
                + "\"path\":\"" + escape(request.getRequestURI()) + "\","
                + "\"details\":[]}");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
