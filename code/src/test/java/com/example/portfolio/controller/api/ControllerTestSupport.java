package com.example.portfolio.controller.api;

import com.example.portfolio.exception.GlobalExceptionHandler;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

// ตัวช่วยสร้าง MockMvc แบบ standalone: ทดสอบ controller + GlobalExceptionHandler + Bean Validation
// โดยไม่ต้องเปิด Spring context และไม่ต้องมีฐานข้อมูล
final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static MockMvc mockMvcFor(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(), // ให้ @AuthenticationPrincipal Long userId ทำงาน
                        new PageableHandlerMethodArgumentResolver())   // ให้ ?page=&size=&sort= กลายเป็น Pageable
                .build();
    }

    // จำลองว่าผู้ใช้ id นี้ login แล้ว (JwtAuthenticationFilter ตัวจริงใส่ userId เป็น principal แบบเดียวกัน)
    static void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    static void logout() {
        SecurityContextHolder.clearContext();
    }
}
