package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.AuthResponse;
import com.example.portfolio.service.AuthResult;

public class AuthMapper {

    private AuthMapper() {
    }

    public static AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), result.user().getId(),
                result.user().getUsername(), result.user().getEmail());
    }
}
