package com.example.portfolio.service.alert.state;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.service.alert.AlertPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// ราคาถึงเงื่อนไขแล้วแต่ยังไม่ได้แจ้ง — handle() คือจุดที่ State pattern ส่งต่อให้ Observer pattern
@Component
@RequiredArgsConstructor
public class TriggeredState implements AlertState {

    private final AlertPublisher alertPublisher;

    @Override
    public AlertStatus status() {
        return AlertStatus.TRIGGERED;
    }

    @Override
    public void handle(PriceAlert alert, BigDecimal currentPrice) {
        alertPublisher.publish(alert, currentPrice);
        alert.setStatus(AlertStatus.NOTIFIED);
    }
}
