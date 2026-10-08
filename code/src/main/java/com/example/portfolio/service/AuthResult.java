package com.example.portfolio.service;

import com.example.portfolio.domain.entity.User;

// ผลลัพธ์ของการสมัคร/เข้าสู่ระบบในชั้น service — controller แปลงเป็น AuthResponse ผ่าน AuthMapper
// service จึงไม่ต้องรู้จัก DTO ของ API (ไม่ข้าม layer ขึ้นไปหา presentation)
public record AuthResult(User user, String token) {
}
