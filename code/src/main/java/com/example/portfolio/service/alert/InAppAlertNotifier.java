package com.example.portfolio.service.alert;

import com.example.portfolio.domain.entity.PriceAlert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InAppAlertNotifier implements AlertObserver {

    @Override
    public void onAlertTriggered(PriceAlert alert, BigDecimal currentPrice) {
        // TODO: เชื่อม WebSocket/Notification table จริงตอนทำ frontend เต็มรูปแบบ
        System.out.printf("[IN-APP] Alert #%d asset=%s currentPrice=%s%n",
                alert.getId(), alert.getAsset().getSymbol(), currentPrice);
    }
}
