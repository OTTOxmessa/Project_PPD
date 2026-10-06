package com.example.portfolio.service;

public interface AuthService {

    AuthResult register(String username, String email, String rawPassword);

    AuthResult login(String email, String rawPassword);
}
