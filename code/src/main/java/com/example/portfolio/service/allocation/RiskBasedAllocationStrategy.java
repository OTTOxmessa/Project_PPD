package com.example.portfolio.service.allocation;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.AssetType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ปรับน้ำหนักตามความเสี่ยงของ asset type ก่อนคำนวณสัดส่วน (ตัวอย่าง: CRYPTO ให้น้ำหนักเสี่ยงสูงสุด)
// ค่า RISK_WEIGHT เป็นตัวอย่างเริ่มต้นเท่านั้น ทีมสามารถปรับสูตร/ค่าจริงตามหลักการวิเคราะห์ความเสี่ยงที่ต้องการนำเสนอได้
@Component
public class RiskBasedAllocationStrategy implements AllocationStrategy {

    @Override
    public String key() {
        return "risk";
    }

    private static final Map<AssetType, BigDecimal> RISK_WEIGHT = Map.of(
            AssetType.CASH, BigDecimal.valueOf(0.2),
            AssetType.BOND, BigDecimal.valueOf(0.5),
            AssetType.MUTUAL_FUND, BigDecimal.valueOf(0.8),
            AssetType.ETF, BigDecimal.valueOf(0.9),
            AssetType.STOCK, BigDecimal.valueOf(1.0),
            AssetType.CRYPTO, BigDecimal.valueOf(1.5)
    );

    @Override
    public Map<Long, BigDecimal> calculateCurrentAllocation(List<Holding> holdings, Map<Long, BigDecimal> latestPrices) {
        Map<Long, BigDecimal> riskAdjustedValues = new HashMap<>();
        BigDecimal totalRiskAdjustedValue = BigDecimal.ZERO;

        for (Holding h : holdings) {
            BigDecimal price = latestPrices.getOrDefault(h.getAsset().getId(), BigDecimal.ZERO);
            BigDecimal riskWeight = RISK_WEIGHT.getOrDefault(h.getAsset().getAssetType(), BigDecimal.ONE);
            BigDecimal riskAdjustedValue = h.getQuantity().multiply(price).multiply(riskWeight);
            riskAdjustedValues.put(h.getAsset().getId(), riskAdjustedValue);
            totalRiskAdjustedValue = totalRiskAdjustedValue.add(riskAdjustedValue);
        }

        Map<Long, BigDecimal> result = new HashMap<>();
        if (totalRiskAdjustedValue.compareTo(BigDecimal.ZERO) == 0) {
            return result;
        }
        for (Map.Entry<Long, BigDecimal> entry : riskAdjustedValues.entrySet()) {
            BigDecimal percent = entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalRiskAdjustedValue, 4, RoundingMode.HALF_UP);
            result.put(entry.getKey(), percent);
        }
        return result;
    }
}
