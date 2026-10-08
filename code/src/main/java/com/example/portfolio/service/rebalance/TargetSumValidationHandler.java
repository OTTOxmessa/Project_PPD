package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Portfolio;

import java.math.BigDecimal;
import java.util.List;

// ห่วงที่ 3 ของ chain: สัดส่วนเป้าหมายต้องรวมได้ 100% (ยอมให้คลาดเคลื่อนจากการปัดเศษ 0.01%)
// ถ้ารวมไม่ครบ แผนจะซื้อ/ขายไม่สมดุล เงินจากการขายไม่พอซื้อ หรือเหลือเงินสดค้าง
public class TargetSumValidationHandler extends RebalanceValidationHandler {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    protected void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
        BigDecimal sum = portfolio.getAllocationTargets().stream()
                .map(AllocationTarget::getTargetPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.subtract(HUNDRED).abs().compareTo(TOLERANCE) > 0) {
            throw new IllegalStateException("สัดส่วนเป้าหมายรวมกันได้ " + sum.stripTrailingZeros().toPlainString()
                    + "% ต้องเท่ากับ 100% ก่อนรีบาลานซ์");
        }
    }
}
