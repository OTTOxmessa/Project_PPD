package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.enums.RebalanceMethod;
import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Rebalance เฉพาะ asset ที่สัดส่วนจริงเบี่ยงจาก target เกิน threshold ที่กำหนด (ค่าเริ่มต้น 5%)
// เหมาะกับพอร์ตที่ไม่ต้องการเทรดถี่เกินไป (ลดค่าธรรมเนียม)
@Component
public class ThresholdRebalanceStrategy implements RebalanceStrategy {

    @Override
    public String key() {
        return "threshold";
    }

    @Override
    public RebalanceMethod method() {
        return RebalanceMethod.THRESHOLD;
    }

    private static final BigDecimal DRIFT_THRESHOLD_PERCENT = BigDecimal.valueOf(5);

    @Override
    public List<TradeOrder> computeTrades(List<Holding> holdings, List<AllocationTarget> targets,
                                           Map<Long, BigDecimal> latestPrices) {

        BigDecimal totalValue = totalPortfolioValue(holdings, latestPrices);
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        Map<Long, BigDecimal> targetPercentByAsset = new HashMap<>();
        for (AllocationTarget t : targets) {
            targetPercentByAsset.put(t.getAsset().getId(), t.getTargetPercent());
        }

        List<TradeOrder> orders = new ArrayList<>();
        for (Holding h : holdings) {
            Long assetId = h.getAsset().getId();
            BigDecimal price = latestPrices.getOrDefault(assetId, BigDecimal.ZERO);
            if (price.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal currentValue = h.getQuantity().multiply(price);
            BigDecimal currentPercent = currentValue.multiply(BigDecimal.valueOf(100))
                    .divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal targetPercent = targetPercentByAsset.getOrDefault(assetId, BigDecimal.ZERO);
            BigDecimal drift = currentPercent.subtract(targetPercent).abs();

            if (drift.compareTo(DRIFT_THRESHOLD_PERCENT) > 0) {
                BigDecimal targetValue = totalValue.multiply(targetPercent)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal diffValue = targetValue.subtract(currentValue);
                BigDecimal diffQuantity = diffValue.divide(price, 6, RoundingMode.HALF_UP).abs();

                TransactionType type = diffValue.compareTo(BigDecimal.ZERO) > 0
                        ? TransactionType.BUY
                        : TransactionType.SELL;

                orders.add(new TradeOrder(assetId, h.getAsset().getSymbol(), type, diffQuantity, price));
            }
        }
        return orders;
    }

    private BigDecimal totalPortfolioValue(List<Holding> holdings, Map<Long, BigDecimal> latestPrices) {
        BigDecimal total = BigDecimal.ZERO;
        for (Holding h : holdings) {
            BigDecimal price = latestPrices.getOrDefault(h.getAsset().getId(), BigDecimal.ZERO);
            total = total.add(h.getQuantity().multiply(price));
        }
        return total;
    }
}
