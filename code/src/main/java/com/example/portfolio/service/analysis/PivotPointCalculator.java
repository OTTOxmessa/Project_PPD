package com.example.portfolio.service.analysis;

import com.example.portfolio.domain.entity.PriceHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// สูตร Pivot Point แบบคลาสสิก: Pivot = (High+Low+Close)/3
// Support = 2xPivot - High, Resistance = 2xPivot - Low
// ใช้แท่งราคาล่าสุด (แถวสุดท้ายในลิสต์ที่ query แบบเรียงจากเก่า -> ใหม่)
@Component
public class PivotPointCalculator implements SupportResistanceStrategy {

    @Override
    public String key() {
        return "pivot";
    }

    @Override
    public SupportResistanceLevels calculate(List<PriceHistory> priceHistory) {
        if (priceHistory.isEmpty()) {
            throw new IllegalArgumentException("ไม่มีข้อมูลราคาให้คำนวณ");
        }
        PriceHistory latest = priceHistory.get(priceHistory.size() - 1);

        BigDecimal pivot = latest.getHigh().add(latest.getLow()).add(latest.getClose())
                .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
        BigDecimal support = pivot.multiply(BigDecimal.valueOf(2)).subtract(latest.getHigh());
        BigDecimal resistance = pivot.multiply(BigDecimal.valueOf(2)).subtract(latest.getLow());

        return new SupportResistanceLevels(support, resistance, pivot);
    }
}
