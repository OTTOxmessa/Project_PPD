package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.PerformanceReportResponse;
import com.example.portfolio.service.performance.PerformanceReport;

public class PerformanceReportMapper {

    private PerformanceReportMapper() {
    }

    public static PerformanceReportResponse toResponse(PerformanceReport report) {
        return new PerformanceReportResponse(
                report.portfolioId(), report.benchmarkCode(), report.from(), report.to(),
                report.portfolioReturnPercent(), report.benchmarkReturnPercent(), report.outperformancePercent());
    }
}
