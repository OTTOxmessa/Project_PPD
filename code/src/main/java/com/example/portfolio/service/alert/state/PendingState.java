package com.example.portfolio.service.alert.state;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.service.alert.condition.AlertConditionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// สถานะเริ่มต้น — มีทางไปต่อ 2 ทาง:
//   ราคาถึงเงื่อนไข -> TRIGGERED
//   รอนานเกินกำหนด (alert.expire-after-days, ค่าเริ่มต้น 90 วัน) -> EXPIRED ไม่ต้องตรวจอีก
@Component
public class PendingState implements AlertState {

    private final Map<AlertCondition, AlertConditionEvaluator> evaluators = new EnumMap<>(AlertCondition.class);
    private final long expireAfterDays;

    // Spring ฉีด AlertConditionEvaluator ทุกตัวมาเป็น List
    public PendingState(List<AlertConditionEvaluator> evaluators,
                        @Value("${alert.expire-after-days:90}") long expireAfterDays) {
        for (AlertConditionEvaluator evaluator : evaluators) {
            this.evaluators.put(evaluator.condition(), evaluator);
        }
        this.expireAfterDays = expireAfterDays;
    }

    @Override
    public AlertStatus status() {
        return AlertStatus.PENDING;
    }

    @Override
    public void handle(PriceAlert alert, BigDecimal currentPrice) {
        AlertConditionEvaluator evaluator = evaluators.get(alert.getCondition());
        if (evaluator == null) {
            throw new IllegalStateException("ยังไม่รองรับเงื่อนไข " + alert.getCondition());
        }
        if (evaluator.isMet(currentPrice, alert.getTargetPrice())) {
            alert.setStatus(AlertStatus.TRIGGERED);
        } else if (isExpired(alert)) {
            alert.setStatus(AlertStatus.EXPIRED);
        }
        // ยังไม่ถึงเงื่อนไขและยังไม่หมดอายุ -> คงสถานะ PENDING ต่อไป
    }

    private boolean isExpired(PriceAlert alert) {
        return alert.getCreatedAt() != null
                && alert.getCreatedAt().isBefore(LocalDateTime.now().minusDays(expireAfterDays));
    }
}
