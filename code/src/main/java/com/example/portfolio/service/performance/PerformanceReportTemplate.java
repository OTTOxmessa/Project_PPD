package com.example.portfolio.service.performance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// Template Method pattern: กำหนดลำดับขั้นตอนคงที่ของการสร้างรายงานเปรียบเทียบผลตอบแทน
// Subclass ปรับได้แค่ "วิธีดึงมูลค่าพอร์ต" กับ "วิธีดึงมูลค่า benchmark" เท่านั้น ลำดับขั้นตอนห้ามเปลี่ยน
public abstract class PerformanceReportTemplate {

    // เมธอดหลักที่ Controller เรียก — ลำดับขั้นตอนตายตัว (final กันไม่ให้ subclass override ลำดับ)
    public final PerformanceReport generateReport(Long portfolioId, String benchmarkCode,
                                                   LocalDate from, LocalDate to) {
        BigDecimal startPortfolioValue = getPortfolioValue(portfolioId, from);
        BigDecimal endPortfolioValue = getPortfolioValue(portfolioId, to);
        BigDecimal startBenchmarkValue = getBenchmarkValue(benchmarkCode, from);
        BigDecimal endBenchmarkValue = getBenchmarkValue(benchmarkCode, to);

        BigDecimal portfolioReturn = calculateReturnPercent(startPortfolioValue, endPortfolioValue);
        BigDecimal benchmarkReturn = calculateReturnPercent(startBenchmarkValue, endBenchmarkValue);

        return new PerformanceReport(
                portfolioId, benchmarkCode, from, to,
                portfolioReturn, benchmarkReturn,
                portfolioReturn.subtract(benchmarkReturn));
    }

    // จุดที่ subclass ต้อง implement เอง (hook methods)
    protected abstract BigDecimal getPortfolioValue(Long portfolioId, LocalDate date);

    protected abstract BigDecimal getBenchmarkValue(String benchmarkCode, LocalDate date);

    // สูตรคำนวณ % ผลตอบแทนใช้สูตรเดียวกันเสมอ ไม่ให้ subclass แก้ (ป้องกันสูตรผิดเพี้ยนไปคนละแบบ)
    private BigDecimal calculateReturnPercent(BigDecimal startValue, BigDecimal endValue) {
        if (startValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return endValue.subtract(startValue)
                .multiply(BigDecimal.valueOf(100))
                .divide(startValue, 4, RoundingMode.HALF_UP);
    }
}
