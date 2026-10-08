package com.example.portfolio.service.alert;

import com.example.portfolio.domain.entity.PriceAlert;

import java.math.BigDecimal;

// สิ่งที่ TriggeredState ต้องการ: "ประกาศว่า alert นี้ทำงานแล้ว" — ไม่ต้องรู้ว่าส่งต่อให้ใครบ้าง (DIP)
public interface AlertPublisher {

    void publish(PriceAlert alert, BigDecimal currentPrice);
}
