package com.example.portfolio.service.performance;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.IndexPriceHistory;
import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.IndexPriceHistoryRepository;
import com.example.portfolio.repository.MarketIndexRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// ทดสอบ Template Method (ลำดับคำนวณตายตัว) และ BenchmarkComparisonService ที่ implement hook จริง
class PerformanceReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 31);
    private static final LocalDate TO = LocalDate.of(2026, 6, 30);

    // subclass จำลองที่คืนมูลค่าตายตัว ใช้ทดสอบสูตรใน template โดยไม่ต้องมีฐานข้อมูล
    private static PerformanceReportTemplate fixedValues(Map<LocalDate, String> portfolio, Map<LocalDate, String> benchmark) {
        return new PerformanceReportTemplate() {
            @Override
            protected BigDecimal getPortfolioValue(Long portfolioId, LocalDate date) {
                return new BigDecimal(portfolio.get(date));
            }

            @Override
            protected BigDecimal getBenchmarkValue(String benchmarkCode, LocalDate date) {
                return new BigDecimal(benchmark.get(date));
            }
        };
    }

    @Nested
    @DisplayName("PerformanceReportTemplate")
    class Template {

        @Test
        @DisplayName("พอร์ต +10% ตลาด +4% → ชนะตลาด 6%")
        void outperformance() {
            PerformanceReport report = fixedValues(
                    Map.of(FROM, "1000", TO, "1100"),
                    Map.of(FROM, "1500", TO, "1560"))
                    .generateReport(1L, "SPX", FROM, TO);

            assertThat(report.portfolioReturnPercent()).isEqualByComparingTo("10");
            assertThat(report.benchmarkReturnPercent()).isEqualByComparingTo("4");
            assertThat(report.outperformancePercent()).isEqualByComparingTo("6");
            assertThat(report.benchmarkCode()).isEqualTo("SPX");
        }

        @Test
        @DisplayName("พอร์ตขาดทุน −5% ตลาด +5% → แพ้ตลาด 10%")
        void underperformance() {
            PerformanceReport report = fixedValues(
                    Map.of(FROM, "2000", TO, "1900"),
                    Map.of(FROM, "100", TO, "105"))
                    .generateReport(1L, "SPX", FROM, TO);

            assertThat(report.outperformancePercent()).isEqualByComparingTo("-10");
        }

        @Test
        @DisplayName("มูลค่าเริ่มต้นเป็น 0 → ผลตอบแทน 0% (ไม่หารด้วยศูนย์)")
        void zeroStartValue() {
            PerformanceReport report = fixedValues(
                    Map.of(FROM, "0", TO, "500"),
                    Map.of(FROM, "100", TO, "110"))
                    .generateReport(1L, "SPX", FROM, TO);

            assertThat(report.portfolioReturnPercent()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("BenchmarkComparisonService")
    class Benchmark {
        @Mock
        private TransactionRepository transactionRepository;
        @Mock
        private PriceHistoryRepository priceHistoryRepository;
        @Mock
        private MarketIndexRepository marketIndexRepository;
        @Mock
        private IndexPriceHistoryRepository indexPriceHistoryRepository;
        @InjectMocks
        private BenchmarkComparisonService service;

        private final Asset aapl = Asset.builder().id(1L).symbol("AAPL").build();

        private Transaction tx(TransactionType type, String qty) {
            return Transaction.builder().asset(aapl).type(type).quantity(new BigDecimal(qty)).price(BigDecimal.ONE).build();
        }

        @Test
        @DisplayName("มูลค่าพอร์ต = จำนวนสุทธิ (ซื้อ − ขาย) × ราคาปิด ณ วันนั้น แล้วเทียบดัชนี")
        void endToEnd() {
            // ณ FROM ถือ 10 หน่วย @100 = 1,000 ; ณ TO ซื้อ 10 ขาย 4 เหลือ 6 หน่วย @200 = 1,200 → +20%
            when(transactionRepository.findByPortfolioIdAndExecutedAtBetween(eq(1L), any(), eq(FROM.atTime(23, 59, 59))))
                    .thenReturn(List.of(tx(TransactionType.BUY, "10")));
            when(transactionRepository.findByPortfolioIdAndExecutedAtBetween(eq(1L), any(), eq(TO.atTime(23, 59, 59))))
                    .thenReturn(List.of(tx(TransactionType.BUY, "10"), tx(TransactionType.SELL, "4")));
            when(priceHistoryRepository.findTopByAssetIdAndPriceDateLessThanEqualOrderByPriceDateDesc(1L, FROM))
                    .thenReturn(Optional.of(PriceHistory.builder().close(new BigDecimal("100")).build()));
            when(priceHistoryRepository.findTopByAssetIdAndPriceDateLessThanEqualOrderByPriceDateDesc(1L, TO))
                    .thenReturn(Optional.of(PriceHistory.builder().close(new BigDecimal("200")).build()));
            // ดัชนี S&P 500 1,000 → 1,100 = +10%
            when(marketIndexRepository.findByIndexCode("SPX"))
                    .thenReturn(Optional.of(MarketIndex.builder().id(9L).indexCode("SPX").build()));
            when(indexPriceHistoryRepository.findTopByMarketIndexIdAndPriceDateLessThanEqualOrderByPriceDateDesc(9L, FROM))
                    .thenReturn(Optional.of(IndexPriceHistory.builder().closeValue(new BigDecimal("1000")).build()));
            when(indexPriceHistoryRepository.findTopByMarketIndexIdAndPriceDateLessThanEqualOrderByPriceDateDesc(9L, TO))
                    .thenReturn(Optional.of(IndexPriceHistory.builder().closeValue(new BigDecimal("1100")).build()));

            PerformanceReport report = service.generateReport(1L, "SPX", FROM, TO);

            assertThat(report.portfolioReturnPercent()).isEqualByComparingTo("20");
            assertThat(report.benchmarkReturnPercent()).isEqualByComparingTo("10");
            assertThat(report.outperformancePercent()).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("ไม่รู้จักดัชนีที่ขอ → ResourceNotFoundException")
        void unknownBenchmark() {
            when(transactionRepository.findByPortfolioIdAndExecutedAtBetween(eq(1L), any(), any())).thenReturn(List.of());
            when(marketIndexRepository.findByIndexCode("NASDAQ")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateReport(1L, "NASDAQ", FROM, TO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
