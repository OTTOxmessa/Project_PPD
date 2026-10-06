package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.User;
import com.example.portfolio.service.AuthResult;
import com.example.portfolio.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import com.example.portfolio.exception.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ทดสอบ API สมัครสมาชิกและเข้าสู่ระบบ
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new AuthController(authService));
    }

    private static User user(Long id, String username, String email) {
        return User.builder().id(id).username(username).email(email).build();
    }

    @Test
    @DisplayName("POST /auth/register ข้อมูลถูกต้อง → 201 พร้อม token")
    void register() throws Exception {
        when(authService.register("otto", "otto@example.com", "secret123"))
                .thenReturn(new AuthResult(user(7L, "otto", "otto@example.com"), "jwt-token"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otto\",\"email\":\"otto@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    @DisplayName("POST /auth/register อีเมลผิดรูปแบบ + รหัสสั้น → 400 แจ้ง 2 ฟิลด์")
    void registerValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otto\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/register อีเมลซ้ำ → 409 Conflict")
    void registerDuplicate() throws Exception {
        when(authService.register("otto", "otto@example.com", "secret123"))
                .thenThrow(new IllegalStateException("Email นี้ถูกใช้ไปแล้ว"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otto\",\"email\":\"otto@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email นี้ถูกใช้ไปแล้ว"));
    }

    @Test
    @DisplayName("POST /auth/login → 200 พร้อม token")
    void login() throws Exception {
        when(authService.login("demo@portfolio.com", "demo1234"))
                .thenReturn(new AuthResult(user(1L, "demo", "demo@portfolio.com"), "jwt"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@portfolio.com\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    @DisplayName("POST /auth/login รหัสผ่านผิด → 401 Unauthorized")
    void loginWrongPassword() throws Exception {
        when(authService.login("demo@portfolio.com", "wrong")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@portfolio.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
