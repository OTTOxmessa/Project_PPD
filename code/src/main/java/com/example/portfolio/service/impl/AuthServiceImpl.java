package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.User;
import com.example.portfolio.exception.InvalidCredentialsException;
import com.example.portfolio.repository.UserRepository;
import com.example.portfolio.security.TokenProvider;
import com.example.portfolio.service.AuthResult;
import com.example.portfolio.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResult register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username นี้ถูกใช้ไปแล้ว");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email นี้ถูกใช้ไปแล้ว");
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword)) // ไม่เก็บรหัสผ่านดิบเด็ดขาด
                .role("USER")
                .build();
        User saved = userRepository.save(user);
        return new AuthResult(saved, tokenProvider.generateToken(saved.getId(), saved.getEmail()));
    }

    @Override
    public AuthResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResult(user, tokenProvider.generateToken(user.getId(), user.getEmail()));
    }
}
