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

// Rebalance กลับไปตาม target แบบเต็มจำนวนทุกครั้งที่ถูกเรียก ไม่สนว่า drift เยอะหรือน้อย
// เพราะ "ตัดสินใจว่าถึงเวลาหรือยัง" ถูกกำหนดจากปฏิทิน (เช่น cron รายไตรมาส) ที่อื่นแล้ว
// จึงไม่ต้องมี threshold เหมือน ThresholdRebalanceStrategy
@Component
public class CalendarRebalanceStrategy implements RebalanceStrategy {

    @Override
    public String key() {
        return "calendar";
    }

    @Override
    public RebalanceMethod method() {
        return RebalanceMethod.CALENDAR;
    }

    @Override
    public List<TradeOrder> computeTrades(List<Holding> holdings, List<AllocationTarget> targets,
                                           Map<Long, BigDecimal> latestPrices) {

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Holding h : holdings) {
            BigDecimal price = latestPrices.getOrDefault(h.getAsset().getId(), BigDecimal.ZERO);
            totalValue = totalValue.add(h.getQuantity().multiply(price));
        }
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
            BigDecimal targetPercent = targetPercentByAsset.getOrDefault(assetId, BigDecimal.ZERO);
            BigDecimal targetValue = totalValue.multiply(targetPercent)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal diffValue = targetValue.subtract(currentValue);

            if (diffValue.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal diffQuantity = diffValue.divide(price, 6, RoundingMode.HALF_UP).abs();
            TransactionType type = diffValue.compareTo(BigDecimal.ZERO) > 0
                    ? TransactionType.BUY
                    : TransactionType.SELL;

            orders.add(new TradeOrder(assetId, h.getAsset().getSymbol(), type, diffQuantity, price));
        }
        return orders;
    }
}
