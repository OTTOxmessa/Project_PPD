package com.example.portfolio.service.alert.condition;

import com.example.portfolio.domain.enums.AlertCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// แจ้งเมื่อราคาลงถึงหรือต่ำกว่าเป้า
@Component
public class PriceBelowEvaluator implements AlertConditionEvaluator {

    @Override
    public AlertCondition condition() {
        return AlertCondition.PRICE_BELOW;
    }

    @Override
    public boolean isMet(BigDecimal currentPrice, BigDecimal targetPrice) {
        return currentPrice.compareTo(targetPrice) <= 0;
    }
}
