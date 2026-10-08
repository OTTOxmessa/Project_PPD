package com.example.portfolio.service.allocation;

import com.example.portfolio.domain.entity.Holding;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// คำนวณสัดส่วนจริงตามมูลค่าตลาดปัจจุบัน (quantity x latest price) เทียบมูลค่ารวมพอร์ต
// นี่คือวิธีที่ควรใช้เป็น default ของระบบจริง
@Component
public class TargetPercentageAllocationStrategy implements AllocationStrategy {

    @Override
    public String key() {
        return "target";
    }

    @Override
    public Map<Long, BigDecimal> calculateCurrentAllocation(List<Holding> holdings, Map<Long, BigDecimal> latestPrices) {
        Map<Long, BigDecimal> marketValues = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Holding h : holdings) {
            BigDecimal price = latestPrices.getOrDefault(h.getAsset().getId(), BigDecimal.ZERO);
            BigDecimal value = h.getQuantity().multiply(price);
            marketValues.put(h.getAsset().getId(), value);
            totalValue = totalValue.add(value);
        }

        Map<Long, BigDecimal> result = new HashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return result;
        }
        for (Map.Entry<Long, BigDecimal> entry : marketValues.entrySet()) {
            BigDecimal percent = entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalValue, 4, RoundingMode.HALF_UP);
            result.put(entry.getKey(), percent);
        }
        return result;
    }
}
