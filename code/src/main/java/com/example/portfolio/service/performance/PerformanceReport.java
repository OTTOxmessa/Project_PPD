package com.example.portfolio.service.performance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PerformanceReport(
        Long portfolioId,
        String benchmarkCode,
        LocalDate from,
        LocalDate to,
        BigDecimal portfolioReturnPercent,
        BigDecimal benchmarkReturnPercent,
        BigDecimal outperformancePercent // portfolioReturnPercent - benchmarkReturnPercent
) {
}
