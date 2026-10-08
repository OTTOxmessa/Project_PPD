package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.MarketIndexRepository;
import com.example.portfolio.service.PriceHistoryService;
import com.example.portfolio.service.market.RefreshSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// ทดสอบการวนอัปเดตราคาทั้งระบบ: ตัวที่พังไม่ทำให้ตัวอื่นหยุด และสรุปผลถูกต้อง
@ExtendWith(MockitoExtension.class)
class MarketDataRefreshServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private MarketIndexRepository marketIndexRepository;
    @Mock
    private PriceHistoryService priceHistoryService;

    private MarketDataRefreshServiceImpl refreshService;

    @BeforeEach
    void setUp() {
        refreshService = new MarketDataRefreshServiceImpl(assetRepository, marketIndexRepository, priceHistoryService, 0);
    }

    @Test
    @DisplayName("นับเฉพาะตัวที่อัปเดตสำเร็จ ตัวที่ throw ถูกข้ามไป")
    void countsSuccessesAndSurvivesFailures() {
        Asset ptt = Asset.builder().id(1L).symbol("PTT").build();
        Asset broken = Asset.builder().id(2L).symbol("BROKEN").build();
        Asset bond = Asset.builder().id(3L).symbol("TBOND").build();
        MarketIndex set = MarketIndex.builder().id(9L).indexCode("SET").build();
        when(assetRepository.findAll()).thenReturn(List.of(ptt, broken, bond));
        when(priceHistoryService.importRealHistory(ptt)).thenReturn(true);
        when(priceHistoryService.importRealHistory(broken)).thenThrow(new RuntimeException("HTTP 429"));
        when(priceHistoryService.importRealHistory(bond)).thenReturn(false);
        when(marketIndexRepository.findAll()).thenReturn(List.of(set));
        when(priceHistoryService.importRealIndexHistory(set)).thenReturn(true);

        RefreshSummary summary = refreshService.refreshAll();

        assertThat(summary).isEqualTo(new RefreshSummary(1, 3, 1, 1));
    }
}
