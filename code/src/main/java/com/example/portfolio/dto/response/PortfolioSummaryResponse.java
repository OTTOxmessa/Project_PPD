package com.example.portfolio.dto.response;

import java.math.BigDecimal;

// สรุปมูลค่าพอร์ตสำหรับคอลัมน์ซ้ายของหน้าหลัก
public record PortfolioSummaryResponse(
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
