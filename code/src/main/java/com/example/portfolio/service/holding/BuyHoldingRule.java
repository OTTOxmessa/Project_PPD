package com.example.portfolio.service.holding;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ซื้อเพิ่ม: เพิ่มจำนวน และถัวเฉลี่ยต้นทุนแบบถ่วงน้ำหนัก (จำนวนเดิม x ต้นทุนเดิม + จำนวนใหม่ x ราคาใหม่) / จำนวนรวม
@Component
public class BuyHoldingRule implements HoldingUpdateRule {

    @Override
    public TransactionType type() {
        return TransactionType.BUY;
    }

    @Override
    public void apply(Holding holding, BigDecimal quantity, BigDecimal price) {
        BigDecimal oldQty = holding.getQuantity();
        BigDecimal oldCostTotal = oldQty.multiply(holding.getAvgCost());
        BigDecimal newQty = oldQty.add(quantity);
        BigDecimal newAvgCost = newQty.signum() == 0
                ? BigDecimal.ZERO
                : oldCostTotal.add(quantity.multiply(price)).divide(newQty, 4, RoundingMode.HALF_UP);
        holding.setQuantity(newQty);
        holding.setAvgCost(newAvgCost);
    }
}
