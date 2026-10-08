package com.example.portfolio.config;

import com.example.portfolio.security.JwtAuthenticationFilter;
import com.example.portfolio.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ระบบยืนยันตัวตนด้วย JWT ของตัวเอง (AuthService + JwtAuthenticationFilter) ไม่ได้ใช้ระบบ login ของ Spring Security
    // ประกาศ bean นี้ไว้เพื่อไม่ให้ Spring สร้างผู้ใช้ชั่วคราวพร้อมรหัสผ่านสุ่มให้เอง
    // (ข้อความ "Using generated security password" ใน log ตอน start)
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("ระบบนี้ยืนยันตัวตนด้วย JWT ผ่าน /api/v1/auth/login");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ไม่มี token / token หมดอายุ -> ตอบ 401 เป็น JSON รูปแบบ ErrorResponse (ค่า default ของ Spring คือ 403)
                // frontend ดัก 401 แล้วพากลับหน้า login อัตโนมัติ
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        // หน้าเว็บ React ที่ build แล้ว (SpaForwardController + ไฟล์ static) — ข้อมูลจริงยังต้องผ่าน /api/v1 ที่ต้อง login
                        .requestMatchers("/", "/index.html", "/login", "/register", "/assets", "/assets/**",
                                "/portfolios/*", "/favicon.ico", "/vite.svg", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
