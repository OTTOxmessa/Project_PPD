package com.example.portfolio.service;

import com.example.portfolio.domain.entity.PriceAlert;

import java.math.BigDecimal;

// ตรวจราคาเทียบกับ alert แล้วเปลี่ยนสถานะ/แจ้งเตือน (State + Observer อยู่ใน service/alert)
public interface PriceAlertMonitorService {

    // ตรวจ alert หนึ่งตัวกับราคาที่ให้มา: PENDING -> TRIGGERED -> แจ้งเตือน -> NOTIFIED
    void checkAndNotify(PriceAlert alert, BigDecimal currentPrice);

    // ตรวจ alert ที่ยัง PENDING ทั้งหมดกับราคาล่าสุด (PriceAlertScheduler เรียก) — คืนจำนวนที่ตรวจ
    int checkPendingAlerts();
}
