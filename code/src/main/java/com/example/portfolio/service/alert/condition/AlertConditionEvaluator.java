package com.example.portfolio.service.alert.condition;

import com.example.portfolio.domain.enums.AlertCondition;

import java.math.BigDecimal;

// Strategy pattern: วิธีตัดสินว่าราคาถึงเงื่อนไขแจ้งเตือนหรือยัง แยกตามชนิดเงื่อนไข
// PendingState เลือก evaluator จาก condition ของ alert แทนการเขียน switch (OCP)
public interface AlertConditionEvaluator {

    AlertCondition condition();

    boolean isMet(BigDecimal currentPrice, BigDecimal targetPrice);
}
