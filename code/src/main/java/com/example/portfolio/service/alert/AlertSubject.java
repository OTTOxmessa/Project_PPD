package com.example.portfolio.service.alert;

import com.example.portfolio.domain.entity.PriceAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Subject ของ Observer pattern — Spring ฉีดทุก bean ที่ implement AlertObserver มาเป็น List ให้อัตโนมัติ
// เพิ่มช่องทางแจ้งเตือนใหม่ (เช่น SMS, LINE) = สร้างคลาส implement AlertObserver ใหม่ ไม่ต้องแก้ไฟล์นี้ (OCP)
@Component
@RequiredArgsConstructor
public class AlertSubject implements AlertPublisher {

    private final List<AlertObserver> observers;

    @Override
    public void publish(PriceAlert alert, BigDecimal currentPrice) {
        for (AlertObserver observer : observers) {
            observer.onAlertTriggered(alert, currentPrice);
        }
    }
}
