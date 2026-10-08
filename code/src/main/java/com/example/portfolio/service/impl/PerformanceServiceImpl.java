package com.example.portfolio.service.impl;

import com.example.portfolio.service.PerformanceService;
import com.example.portfolio.service.performance.PerformanceReport;
import com.example.portfolio.service.performance.PerformanceReportTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

// ตรวจเงื่อนไขของคำขอ แล้วส่งต่อให้ Template Method สร้างรายงาน
// ขึ้นกับ PerformanceReportTemplate (abstract) ไม่ใช่ BenchmarkComparisonService ตัวจริง (DIP)
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceReportTemplate reportTemplate;

    @Override
    public PerformanceReport compareWithBenchmark(Long portfolioId, String benchmarkCode, LocalDate from, LocalDate to) {
        if (benchmarkCode == null || benchmarkCode.isBlank()) {
            throw new IllegalArgumentException("ต้องระบุดัชนีที่ต้องการเปรียบเทียบ");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("วันเริ่มต้นต้องไม่อยู่หลังวันสิ้นสุด");
        }
        return reportTemplate.generateReport(portfolioId, benchmarkCode.trim(), from, to);
    }
}
