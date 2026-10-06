package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.LoginRequest;
import com.example.portfolio.dto.request.RegisterRequest;
import com.example.portfolio.dto.response.AuthResponse;
import com.example.portfolio.mapper.AuthMapper;
import com.example.portfolio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "สมัครสมาชิกและเข้าสู่ระบบ (ไม่ต้องใช้ token)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthMapper.toResponse(authService.register(request.username(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthMapper.toResponse(authService.login(request.email(), request.password()));
    }
}
