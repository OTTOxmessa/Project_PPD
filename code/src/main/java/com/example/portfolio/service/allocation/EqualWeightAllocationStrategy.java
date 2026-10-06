package com.example.portfolio.service.allocation;

import com.example.portfolio.domain.entity.Holding;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// สมมติทุก asset ควรมีน้ำหนักเท่ากัน ไม่สนมูลค่าตลาดจริง — เหมาะกับพอร์ตที่เน้น diversification ง่าย ๆ
@Component
public class EqualWeightAllocationStrategy implements AllocationStrategy {

    @Override
    public String key() {
        return "equal";
    }

    @Override
    public Map<Long, BigDecimal> calculateCurrentAllocation(List<Holding> holdings, Map<Long, BigDecimal> latestPrices) {
        if (holdings.isEmpty()) {
            return Map.of();
        }
        BigDecimal equalShare = BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(holdings.size()), 4, RoundingMode.HALF_UP);

        Map<Long, BigDecimal> result = new HashMap<>();
        for (Holding h : holdings) {
            result.put(h.getAsset().getId(), equalShare);
        }
        return result;
    }
}
