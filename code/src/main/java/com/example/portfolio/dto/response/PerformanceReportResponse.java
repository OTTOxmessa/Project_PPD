package com.example.portfolio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

// outperformancePercent = portfolioReturnPercent - benchmarkReturnPercent
public record PerformanceReportResponse(
        Long portfolioId,
        String benchmarkCode,
        LocalDate from,
        LocalDate to,
        BigDecimal portfolioReturnPercent,
        BigDecimal benchmarkReturnPercent,
        BigDecimal outperformancePercent
) {
}
