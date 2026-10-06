package com.example.portfolio.service.analysis;

import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.SupportResistanceService;
import com.example.portfolio.service.impl.SupportResistanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบการคำนวณแนวรับ-แนวต้านทั้ง 2 Strategy และ service ที่เลือกใช้
class SupportResistanceTest {

    private static PriceHistory bar(String high, String low, String close) {
        return PriceHistory.builder()
                .open(new BigDecimal(close))
                .high(new BigDecimal(high))
                .low(new BigDecimal(low))
                .close(new BigDecimal(close))
                .build();
    }

    @Nested
    @DisplayName("PivotPointCalculator")
    class PivotPoint {
        private final SupportResistanceStrategy strategy = new PivotPointCalculator();

        @Test
        @DisplayName("H=110 L=90 C=100 → pivot 100, แนวรับ 90, แนวต้าน 110")
        void classicFormula() {
            SupportResistanceLevels levels = strategy.calculate(List.of(bar("110", "90", "100")));

            assertThat(levels.pivot()).isEqualByComparingTo("100");
            assertThat(levels.support()).isEqualByComparingTo("90");
            assertThat(levels.resistance()).isEqualByComparingTo("110");
        }

        @Test
        @DisplayName("ใช้แท่งล่าสุด (ตัวสุดท้ายของลิสต์) เท่านั้น")
        void usesLatestBar() {
            SupportResistanceLevels levels = strategy.calculate(List.of(
                    bar("500", "400", "450"),
                    bar("12", "6", "9")));

            assertThat(levels.pivot()).isEqualByComparingTo("9");
            assertThat(levels.support()).isEqualByComparingTo("6");
            assertThat(levels.resistance()).isEqualByComparingTo("12");
        }

        @Test
        @DisplayName("ไม่มีข้อมูลราคา → IllegalArgumentException")
        void emptyHistory() {
            assertThatThrownBy(() -> strategy.calculate(List.of())).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("MovingAverageBandCalculator")
    class MovingAverageBand {
        private final SupportResistanceStrategy strategy = new MovingAverageBandCalculator();

        @Test
        @DisplayName("ราคาปิด 10, 20, 30 → ค่าเฉลี่ย 20 ± 2×SD (SD ≈ 8.165)")
        void meanPlusMinusTwoSd() {
            SupportResistanceLevels levels = strategy.calculate(List.of(
                    bar("10", "10", "10"), bar("20", "20", "20"), bar("30", "30", "30")));

            assertThat(levels.pivot()).isEqualByComparingTo("20");
            assertThat(levels.support()).isCloseTo(new BigDecimal("3.670"), within(new BigDecimal("0.01")));
            assertThat(levels.resistance()).isCloseTo(new BigDecimal("36.330"), within(new BigDecimal("0.01")));
        }

        @Test
        @DisplayName("ราคาคงที่ → SD = 0 แนวรับ = แนวต้าน = ค่าเฉลี่ย")
        void flatPrices() {
            SupportResistanceLevels levels = strategy.calculate(List.of(
                    bar("50", "50", "50"), bar("50", "50", "50")));

            assertThat(levels.support()).isEqualByComparingTo("50");
            assertThat(levels.resistance()).isEqualByComparingTo("50");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("SupportResistanceServiceImpl")
    class ServiceLayer {
        @Mock
        private PriceHistoryRepository priceHistoryRepository;
        @Mock
        private SupportResistanceStrategy strategy;

        private SupportResistanceService service;

        private final LocalDate from = LocalDate.of(2026, 1, 1);
        private final LocalDate to = LocalDate.of(2026, 3, 31);

        @BeforeEach
        void setUp() {
            when(strategy.key()).thenReturn("pivot");
            service = new SupportResistanceServiceImpl(priceHistoryRepository, List.of(strategy));
        }

        @Test
        @DisplayName("เลือก Strategy จากชื่อ แล้วส่งราคาในช่วงวันที่ให้คำนวณ")
        void delegatesToStrategy() {
            List<PriceHistory> history = List.of(bar("110", "90", "100"));
            SupportResistanceLevels expected = new SupportResistanceLevels(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TWO);
            when(priceHistoryRepository.findByAssetIdAndPriceDateBetweenOrderByPriceDateAsc(1L, from, to)).thenReturn(history);
            when(strategy.calculate(history)).thenReturn(expected);

            assertThat(service.calculate(1L, from, to, "pivot")).isSameAs(expected);
        }

        @Test
        @DisplayName("ไม่มีราคาในช่วงที่ขอ → IllegalStateException และไม่เรียก Strategy คำนวณ")
        void noDataInRange() {
            when(priceHistoryRepository.findByAssetIdAndPriceDateBetweenOrderByPriceDateAsc(1L, from, to)).thenReturn(List.of());

            assertThatThrownBy(() -> service.calculate(1L, from, to, "pivot")).isInstanceOf(IllegalStateException.class);
            verify(strategy, never()).calculate(any());
        }

        @Test
        @DisplayName("ชื่อวิธีที่ไม่รู้จัก หรือวันเริ่มหลังวันจบ → IllegalArgumentException (400)")
        void invalidRequest() {
            assertThatThrownBy(() -> service.calculate(1L, from, to, "fibonacci")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.calculate(1L, to, from, "pivot")).isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(priceHistoryRepository);
        }
    }
}
