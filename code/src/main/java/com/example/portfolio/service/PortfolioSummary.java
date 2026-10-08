package com.example.portfolio.service;

import java.math.BigDecimal;

// สรุปมูลค่าพอร์ตสำหรับคอลัมน์ซ้ายของหน้าหลัก
public record PortfolioSummary(
        Long id,
        String name,
        String baseCurrency,
        BigDecimal marketValue,
        BigDecimal costBasis,
        BigDecimal gain,
        BigDecimal gainPercent,
        int holdingsCount
) {
}
