package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.User;
import com.example.portfolio.exception.InvalidCredentialsException;
import com.example.portfolio.repository.UserRepository;
import com.example.portfolio.security.TokenProvider;
import com.example.portfolio.service.AuthResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบสมัครสมาชิกและเข้าสู่ระบบ: เข้ารหัสรหัสผ่าน, กันข้อมูลซ้ำ, ตรวจรหัสผ่านผิด
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("register: เก็บรหัสผ่านแบบ hash เท่านั้น และคืน token")
    void registerHashesPassword() {
        when(userRepository.existsByUsername("otto")).thenReturn(false);
        when(userRepository.existsByEmail("otto@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtUtil.generateToken(7L, "otto@example.com")).thenReturn("jwt-token");

        AuthResult result = authService.register("otto", "otto@example.com", "secret123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$hashed").isNotEqualTo("secret123");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("register: username ซ้ำ → IllegalStateException (409)")
    void registerDuplicateUsername() {
        when(userRepository.existsByUsername("otto")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("otto", "new@example.com", "secret123"))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: email ซ้ำ → IllegalStateException (409)")
    void registerDuplicateEmail() {
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(userRepository.existsByEmail("otto@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("newbie", "otto@example.com", "secret123"))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: รหัสผ่านถูก → คืน token")
    void loginSuccess() {
        User user = User.builder().id(3L).username("demo").email("demo@portfolio.com").passwordHash("hash").build();
        when(userRepository.findByEmail("demo@portfolio.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("demo1234", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(3L, "demo@portfolio.com")).thenReturn("jwt");

        AuthResult result = authService.login("demo@portfolio.com", "demo1234");

        assertThat(result.token()).isEqualTo("jwt");
        assertThat(result.user().getUsername()).isEqualTo("demo");
    }

    @Test
    @DisplayName("login: รหัสผ่านผิด → InvalidCredentialsException (401) และไม่ออก token")
    void loginWrongPassword() {
        User user = User.builder().id(3L).email("demo@portfolio.com").passwordHash("hash").build();
        when(userRepository.findByEmail("demo@portfolio.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("demo@portfolio.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtUtil, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("login: ไม่พบอีเมล → ข้อความเดียวกับรหัสผ่านผิด (ไม่บอกว่าอีเมลมีบัญชีหรือไม่)")
    void loginUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "x"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(new InvalidCredentialsException().getMessage());
    }
}
