package com.example.portfolio.service;

import com.example.portfolio.service.market.ExchangeRate;

import java.util.Optional;

// อัตรา USD/THB สำหรับแสดงผลเป็นเงินบาทบนหน้าเว็บอย่างเดียว
// ข้อมูลและการคำนวณทั้งหมดในระบบยังเป็น USD — ไม่มีการบันทึกค่าที่แปลงแล้วลงฐานข้อมูล
public interface ExchangeRateService {

    // empty = ยังไม่เคยดึงอัตราได้เลย (Yahoo ใช้ไม่ได้ตั้งแต่เริ่มแอป)
    Optional<ExchangeRate> usdToThb();
}
