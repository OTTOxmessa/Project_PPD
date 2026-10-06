package com.example.portfolio.dto.response;

public record AuthResponse(String token, Long userId, String username, String email) {
}
