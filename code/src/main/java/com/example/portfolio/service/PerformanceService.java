package com.example.portfolio.service;

import com.example.portfolio.service.performance.PerformanceReport;

import java.time.LocalDate;

// ฟีเจอร์ 4: เปรียบเทียบผลตอบแทนของพอร์ตกับดัชนีตลาด
public interface PerformanceService {

    PerformanceReport compareWithBenchmark(Long portfolioId, String benchmarkCode, LocalDate from, LocalDate to);
}
