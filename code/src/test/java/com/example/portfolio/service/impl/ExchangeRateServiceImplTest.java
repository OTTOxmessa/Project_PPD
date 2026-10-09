package com.example.portfolio.service.impl;

import com.example.portfolio.service.market.ExchangeRate;
import com.example.portfolio.service.market.HistoricalPriceSource;
import com.example.portfolio.service.market.PriceBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบอัตรา USD/THB: ใช้ราคาปิดวันล่าสุดของ THB=X, จำค่าไว้, ดึงไม่ได้ใช้ค่าเดิม
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private HistoricalPriceSource priceSource;

    private static PriceBar bar(String date, String close) {
        BigDecimal c = close == null ? null : new BigDecimal(close);
        return new PriceBar(LocalDate.parse(date), c, c, c, c, 0L);
    }

    @Test
    @DisplayName("ใช้ราคาปิดวันล่าสุด (ข้ามวันที่ไม่มีราคา) เป็นอัตรา 1 USD = x THB")
    void usesLatestClose() {
        when(priceSource.fetchDailyHistory("THB=X")).thenReturn(List.of(
                bar("2026-10-07", "32.40"), bar("2026-10-08", "32.85"), bar("2026-10-09", null)));

        ExchangeRate rate = new ExchangeRateServiceImpl(priceSource, 60).usdToThb().orElseThrow();

        assertThat(rate.base()).isEqualTo("USD");
        assertThat(rate.quote()).isEqualTo("THB");
        assertThat(rate.rate()).isEqualByComparingTo("32.85");
        assertThat(rate.asOf()).isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @Test
    @DisplayName("เรียกซ้ำภายในเวลาที่จำไว้ → ดึงจาก Yahoo ครั้งเดียว")
    void cachesRate() {
        when(priceSource.fetchDailyHistory("THB=X")).thenReturn(List.of(bar("2026-10-08", "32.85")));
        ExchangeRateServiceImpl service = new ExchangeRateServiceImpl(priceSource, 60);

        service.usdToThb();
        service.usdToThb();

        verify(priceSource, times(1)).fetchDailyHistory("THB=X");
    }

    @Test
    @DisplayName("หมดเวลาแล้วดึงรอบใหม่ไม่สำเร็จ → ใช้อัตราล่าสุดที่เคยได้ต่อไป")
    void keepsLastRateWhenRefreshFails() {
        when(priceSource.fetchDailyHistory("THB=X"))
                .thenReturn(List.of(bar("2026-10-08", "32.85")))
                .thenReturn(List.of());
        ExchangeRateServiceImpl service = new ExchangeRateServiceImpl(priceSource, 0); // 0 นาที = หมดอายุทันที

        service.usdToThb();
        ExchangeRate rate = service.usdToThb().orElseThrow();

        assertThat(rate.rate()).isEqualByComparingTo("32.85");
        verify(priceSource, times(2)).fetchDailyHistory("THB=X");
    }

    @Test
    @DisplayName("ยังไม่เคยดึงได้เลย → empty (หน้าเว็บแสดงเป็น USD)")
    void emptyWhenNeverFetched() {
        when(priceSource.fetchDailyHistory("THB=X")).thenReturn(List.of());

        assertThat(new ExchangeRateServiceImpl(priceSource, 60).usdToThb()).isEmpty();
    }
}
