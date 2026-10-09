package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.PerformanceReportResponse;
import com.example.portfolio.mapper.PerformanceReportMapper;
import com.example.portfolio.service.PerformanceService;
import com.example.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Performance", description = "เปรียบเทียบผลตอบแทนของพอร์ตกับดัชนีตลาด")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PortfolioService portfolioService;

    // ตัวอย่าง: GET /performance?benchmark=SPX&from=2026-01-05&to=2026-09-24 (SPX = S&P 500, DJI = Dow Jones)
    @GetMapping
    public PerformanceReportResponse getReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long portfolioId,
            @RequestParam String benchmark,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return PerformanceReportMapper.toResponse(
                performanceService.compareWithBenchmark(portfolioId, benchmark, from, to));
    }
}
