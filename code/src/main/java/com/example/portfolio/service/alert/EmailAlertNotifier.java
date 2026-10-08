package com.example.portfolio.service.alert;

import com.example.portfolio.domain.entity.PriceAlert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EmailAlertNotifier implements AlertObserver {

    @Override
    public void onAlertTriggered(PriceAlert alert, BigDecimal currentPrice) {
        // TODO: เชื่อม JavaMailSender จริงตอนทำระบบส่งอีเมล — log ไว้ก่อนเพื่อให้เห็น flow ทำงานถูกต้อง
        System.out.printf("[EMAIL] Alert #%d asset=%s condition=%s target=%s currentPrice=%s%n",
                alert.getId(), alert.getAsset().getSymbol(), alert.getCondition(),
                alert.getTargetPrice(), currentPrice);
    }
}
