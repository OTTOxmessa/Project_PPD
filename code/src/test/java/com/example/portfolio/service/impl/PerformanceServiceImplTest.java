package com.example.portfolio.service.impl;

import com.example.portfolio.service.performance.PerformanceReport;
import com.example.portfolio.service.performance.PerformanceReportTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบการตรวจคำขอก่อนส่งต่อให้ Template Method สร้างรายงาน
@ExtendWith(MockitoExtension.class)
class PerformanceServiceImplTest {

    @Mock
    private PerformanceReportTemplate reportTemplate;

    @InjectMocks
    private PerformanceServiceImpl performanceService;

    private static final LocalDate FROM = LocalDate.of(2026, 1, 5);
    private static final LocalDate TO = LocalDate.of(2026, 9, 24);

    @Test
    @DisplayName("คำขอถูกต้อง → ส่งต่อให้ template สร้างรายงาน")
    void delegatesToTemplate() {
        PerformanceReport report = new PerformanceReport(1L, "SET", FROM, TO,
                BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"));
        when(reportTemplate.generateReport(1L, "SET", FROM, TO)).thenReturn(report);

        assertThat(performanceService.compareWithBenchmark(1L, " SET ", FROM, TO)).isSameAs(report);
    }

    @Test
    @DisplayName("วันเริ่มอยู่หลังวันจบ → IllegalArgumentException (400) ไม่คำนวณ")
    void fromAfterTo() {
        assertThatThrownBy(() -> performanceService.compareWithBenchmark(1L, "SET", TO, FROM))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(reportTemplate);
    }

    @Test
    @DisplayName("ไม่ระบุดัชนี → IllegalArgumentException (400)")
    void blankBenchmark() {
        assertThatThrownBy(() -> performanceService.compareWithBenchmark(1L, " ", FROM, TO))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(reportTemplate);
    }
}
