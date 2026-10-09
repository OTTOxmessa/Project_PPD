package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.service.holding.BuyHoldingRule;
import com.example.portfolio.service.holding.SellHoldingRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบการคำนวณ holding ผ่าน HoldingUpdateRule: ถัวเฉลี่ยต้นทุนตอนซื้อ และลดจำนวนตอนขาย
@ExtendWith(MockitoExtension.class)
class HoldingServiceImplTest {

    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;

    private HoldingServiceImpl holdingService;

    @BeforeEach
    void setUp() {
        holdingService = new HoldingServiceImpl(holdingRepository, portfolioRepository, assetRepository,
                List.of(new BuyHoldingRule(), new SellHoldingRule()));
    }

    private static Holding holding(String qty, String avgCost) {
        return Holding.builder()
                .portfolio(Portfolio.builder().id(1L).build())
                .asset(Asset.builder().id(10L).symbol("AAPL").build())
                .quantity(new BigDecimal(qty))
                .avgCost(new BigDecimal(avgCost))
                .build();
    }

    private void saveReturnsArgument() {
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("BUY ครั้งแรก: สร้าง holding ใหม่ ต้นทุนเฉลี่ย = ราคาที่ซื้อ")
    void firstBuyCreatesHolding() {
        when(holdingRepository.findByPortfolioIdAndAssetId(1L, 10L)).thenReturn(Optional.empty());
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(Portfolio.builder().id(1L).build()));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(Asset.builder().id(10L).symbol("AAPL").build()));
        saveReturnsArgument();

        Holding result = holdingService.applyTransaction(1L, 10L, TransactionType.BUY,
                new BigDecimal("100"), new BigDecimal("35.50"));

        assertThat(result.getQuantity()).isEqualByComparingTo("100");
        assertThat(result.getAvgCost()).isEqualByComparingTo("35.50");
        assertThat(result.getAsset().getSymbol()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("BUY เพิ่ม: ต้นทุนเฉลี่ยถ่วงน้ำหนัก (100@10 + 100@20 = 200@15)")
    void buyMoreUsesWeightedAverage() {
        when(holdingRepository.findByPortfolioIdAndAssetId(1L, 10L)).thenReturn(Optional.of(holding("100", "10")));
        saveReturnsArgument();

        Holding result = holdingService.applyTransaction(1L, 10L, TransactionType.BUY,
                new BigDecimal("100"), new BigDecimal("20"));

        assertThat(result.getQuantity()).isEqualByComparingTo("200");
        assertThat(result.getAvgCost()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("SELL: ลดจำนวน แต่ต้นทุนเฉลี่ยเท่าเดิม")
    void sellKeepsAverageCost() {
        when(holdingRepository.findByPortfolioIdAndAssetId(1L, 10L)).thenReturn(Optional.of(holding("100", "12.5")));
        saveReturnsArgument();

        Holding result = holdingService.applyTransaction(1L, 10L, TransactionType.SELL,
                new BigDecimal("40"), new BigDecimal("30"));

        assertThat(result.getQuantity()).isEqualByComparingTo("60");
        assertThat(result.getAvgCost()).isEqualByComparingTo("12.5");
    }

    @Test
    @DisplayName("SELL เกินจำนวนที่ถือ → IllegalStateException และไม่บันทึก")
    void sellMoreThanOwned() {
        when(holdingRepository.findByPortfolioIdAndAssetId(1L, 10L)).thenReturn(Optional.of(holding("10", "50")));

        assertThatThrownBy(() -> holdingService.applyTransaction(1L, 10L, TransactionType.SELL,
                new BigDecimal("11"), new BigDecimal("50")))
                .isInstanceOf(IllegalStateException.class);
        verify(holdingRepository, never()).save(any());
    }

    @Test
    @DisplayName("ประเภทที่ไม่มี rule (DIVIDEND) → IllegalArgumentException ก่อนแตะฐานข้อมูล")
    void unsupportedTypeRejected() {
        assertThatThrownBy(() -> holdingService.applyTransaction(1L, 10L, TransactionType.DIVIDEND,
                BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(holdingRepository);
    }

    @Test
    @DisplayName("getOpenPositions: ไม่คืนสินทรัพย์ที่ขายหมดแล้ว (จำนวน 0)")
    void openPositionsSkipSoldOut() {
        Holding open = holding("10", "50");
        Holding soldOut = holding("0", "40");
        when(holdingRepository.findByPortfolioId(1L)).thenReturn(List.of(open, soldOut));

        assertThat(holdingService.getOpenPositions(1L)).containsExactly(open);
    }
}
