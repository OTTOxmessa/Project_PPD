package com.example.portfolio.service.alert.state;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;

import java.math.BigDecimal;

// State pattern: แต่ละสถานะของ PriceAlert รู้วิธีจัดการตัวเองว่าจะเปลี่ยนไปสถานะไหนต่อ
// PriceAlertMonitorService เลือก state จาก status() แทนการเขียน if-else ตามสถานะ
// interface มีแค่ 2 เมธอดที่ทุกสถานะใช้จริง (ISP) — ทุก implementation ใช้แทนกันได้ (LSP)
public interface AlertState {

    // สถานะที่คลาสนี้รับผิดชอบ
    AlertStatus status();

    void handle(PriceAlert alert, BigDecimal currentPrice);
}
