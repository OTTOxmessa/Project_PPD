package com.example.portfolio.service.holding;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// ขาย: ลดจำนวน ต้นทุนเฉลี่ยคงเดิมตาม convention ทั่วไป และห้ามขายเกินจำนวนที่ถือ
@Component
public class SellHoldingRule implements HoldingUpdateRule {

    @Override
    public TransactionType type() {
        return TransactionType.SELL;
    }

    @Override
    public void apply(Holding holding, BigDecimal quantity, BigDecimal price) {
        BigDecimal newQty = holding.getQuantity().subtract(quantity);
        if (newQty.signum() < 0) {
            throw new IllegalStateException("ขายเกินจำนวนที่ถืออยู่ (มี " + holding.getQuantity() + ")");
        }
        holding.setQuantity(newQty);
    }
}
