package com.example.portfolio.service.alert.state;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// สถานะสุดท้าย (terminal) — PendingState ส่งมาที่นี่เมื่อ alert รอนานเกินกำหนดโดยราคาไม่ถึงเป้า
// ไม่เปลี่ยนสถานะอีกแล้ว และ scheduler จะไม่ดึง alert นี้มาตรวจอีก (ตรวจเฉพาะ PENDING)
@Component
public class ExpiredState implements AlertState {

    @Override
    public AlertStatus status() {
        return AlertStatus.EXPIRED;
    }

    @Override
    public void handle(PriceAlert alert, BigDecimal currentPrice) {
        // ไม่ทำอะไร — สถานะสุดท้ายแล้ว
    }
}
