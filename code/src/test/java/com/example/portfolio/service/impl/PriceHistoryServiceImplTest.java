package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.IndexPriceHistoryRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.market.HistoricalPriceSource;
import com.example.portfolio.service.market.PriceBar;
import com.example.portfolio.service.market.PriceHistoryFallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบการนำเข้าราคาดัชนี: ถ้าแหล่งภายนอกส่งข้อมูลมาน้อยเกินไป ต้องเก็บประวัติเดิมไว้ ไม่ลบทิ้ง
@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceImplTest {

    @Mock
    private HistoricalPriceSource priceSource;
    @Mock
    private PriceHistoryFallback fallback;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private IndexPriceHistoryRepository indexPriceHistoryRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private PriceHistoryServiceImpl service;

    private final MarketIndex set = MarketIndex.builder().id(1L).indexCode("SPX").name("S&P 500").build();

    @BeforeEach
    void setUp() {
        service = new PriceHistoryServiceImpl(priceSource, fallback, priceHistoryRepository,
                indexPriceHistoryRepository, assetRepository, transactionManager);
        when(priceSource.resolveIndexTicker("SPX")).thenReturn(Optional.of("^GSPC"));
    }

    private static List<PriceBar> bars(int days) {
        LocalDate start = LocalDate.of(2026, 1, 1);
        return IntStream.range(0, days)
                .mapToObj(i -> new PriceBar(start.plusDays(i), null, null, null, new BigDecimal("1400"), null))
                .toList();
    }

    @Test
    @DisplayName("Yahoo ส่งดัชนีมาแค่ 1 วัน → ไม่ลบประวัติเดิม และคืน false ให้ใช้ข้อมูลเดิม")
    void keepsExistingHistoryWhenTooShort() {
        when(priceSource.name()).thenReturn("Yahoo Finance");
        when(priceSource.fetchDailyHistory("^GSPC")).thenReturn(bars(1));

        assertThat(service.importRealIndexHistory(set)).isFalse();
        verify(indexPriceHistoryRepository, never()).purgeByMarketIndexId(anyLong());
        verify(indexPriceHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("ได้ข้อมูลครบตามเกณฑ์ → แทนที่ประวัติเดิมด้วยข้อมูลจริง")
    void replacesHistoryWhenLongEnough() {
        when(priceSource.name()).thenReturn("Yahoo Finance");
        when(priceSource.fetchDailyHistory("^GSPC"))
                .thenReturn(bars(PriceHistoryServiceImpl.MIN_INDEX_HISTORY_DAYS));

        assertThat(service.importRealIndexHistory(set)).isTrue();
        verify(indexPriceHistoryRepository).purgeByMarketIndexId(1L);
        verify(indexPriceHistoryRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("ดึงข้อมูลไม่ได้เลย → คืน false และไม่แตะฐานข้อมูล")
    void emptyResponse() {
        when(priceSource.fetchDailyHistory("^GSPC")).thenReturn(List.of());

        assertThat(service.importRealIndexHistory(set)).isFalse();
        verify(indexPriceHistoryRepository, never()).purgeByMarketIndexId(anyLong());
    }
}
