package com.example.portfolio.service.analysis;

import com.example.portfolio.domain.entity.PriceHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

// แนวรับ-แนวต้านจาก Moving Average Band: MA ± 2 x Standard Deviation ของราคาปิดในช่วงที่กำหนด
// (หลักการคล้าย Bollinger Band) เหมาะกับสินทรัพย์ผันผวนสูงกว่าการใช้ pivot point คงที่ตัวเดียว
@Component
public class MovingAverageBandCalculator implements SupportResistanceStrategy {

    @Override
    public String key() {
        return "ma";
    }

    private static final BigDecimal BAND_MULTIPLIER = BigDecimal.valueOf(2);

    @Override
    public SupportResistanceLevels calculate(List<PriceHistory> priceHistory) {
        if (priceHistory.isEmpty()) {
            throw new IllegalArgumentException("ไม่มีข้อมูลราคาให้คำนวณ");
        }

        List<BigDecimal> closes = priceHistory.stream().map(PriceHistory::getClose).toList();
        BigDecimal mean = average(closes);
        BigDecimal stdDev = standardDeviation(closes, mean);
        BigDecimal band = stdDev.multiply(BAND_MULTIPLIER);

        return new SupportResistanceLevels(mean.subtract(band), mean.add(band), mean);
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        return variance.sqrt(new MathContext(10));
    }
}
