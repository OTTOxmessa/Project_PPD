package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AllocationTargetRepository;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.allocation.AllocationComparison;
import com.example.portfolio.service.allocation.EqualWeightAllocationStrategy;
import com.example.portfolio.service.allocation.RiskBasedAllocationStrategy;
import com.example.portfolio.service.allocation.TargetPercentageAllocationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบการเทียบสัดส่วนจริงกับเป้าหมาย, การเลือก Strategy จากชื่อ และการตั้งเป้าหมายรายสินทรัพย์
@ExtendWith(MockitoExtension.class)
class AllocationServiceImplTest {

    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AllocationTargetRepository allocationTargetRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;

    private AllocationServiceImpl allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationServiceImpl(holdingRepository, allocationTargetRepository,
                priceHistoryRepository, portfolioRepository, assetRepository,
                List.of(new TargetPercentageAllocationStrategy(), new EqualWeightAllocationStrategy(),
                        new RiskBasedAllocationStrategy()));
    }

    private static Asset asset(long id, String symbol) {
        return Asset.builder().id(id).symbol(symbol).build();
    }

    private static Holding holding(Asset asset, String qty) {
        return Holding.builder().asset(asset).quantity(new BigDecimal(qty)).avgCost(BigDecimal.ZERO).build();
    }

    private static PriceHistory close(String price) {
        return PriceHistory.builder().close(new BigDecimal(price)).build();
    }

    @Test
    @DisplayName("compare (target): คำนวณ drift = สัดส่วนจริง − เป้าหมาย ต่อสินทรัพย์")
    void compareComputesDrift() {
        Asset aapl = asset(1, "AAPL");
        Asset jpm = asset(2, "JPM");
        when(holdingRepository.findByPortfolioId(9L)).thenReturn(List.of(holding(aapl, "70"), holding(jpm, "30")));
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(1L)).thenReturn(Optional.of(close("10")));
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(2L)).thenReturn(Optional.of(close("10")));
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of(
                AllocationTarget.builder().asset(aapl).targetPercent(new BigDecimal("50")).build(),
                AllocationTarget.builder().asset(jpm).targetPercent(new BigDecimal("50")).build()));

        List<AllocationComparison> result = allocationService.compare(9L, "target");

        AllocationComparison aaplRow = result.stream().filter(r -> r.symbol().equals("AAPL")).findFirst().orElseThrow();
        assertThat(aaplRow.currentPercent()).isEqualByComparingTo("70");
        assertThat(aaplRow.targetPercent()).isEqualByComparingTo("50");
        assertThat(aaplRow.driftPercent()).isEqualByComparingTo("20");
        AllocationComparison jpmRow = result.stream().filter(r -> r.symbol().equals("JPM")).findFirst().orElseThrow();
        assertThat(jpmRow.driftPercent()).isEqualByComparingTo("-20");
    }

    @Test
    @DisplayName("compare (equal): เลือก Strategy จากชื่อ ได้น้ำหนักเท่ากันแม้มูลค่าต่างกัน")
    void compareSelectsStrategyByName() {
        Asset aapl = asset(1, "AAPL");
        Asset jpm = asset(2, "JPM");
        when(holdingRepository.findByPortfolioId(9L)).thenReturn(List.of(holding(aapl, "70"), holding(jpm, "30")));
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(1L)).thenReturn(Optional.of(close("10")));
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(2L)).thenReturn(Optional.of(close("10")));
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of());

        List<AllocationComparison> result = allocationService.compare(9L, "EQUAL");

        assertThat(result).allSatisfy(r -> assertThat(r.currentPercent()).isEqualByComparingTo("50"));
    }

    @Test
    @DisplayName("compare: ชื่อวิธีที่ไม่รู้จัก → IllegalArgumentException (400) ก่อนแตะฐานข้อมูล")
    void compareUnknownMethod() {
        assertThatThrownBy(() -> allocationService.compare(9L, "magic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target");
        verifyNoInteractions(holdingRepository);
    }

    @Test
    @DisplayName("compare: สินทรัพย์ที่ไม่ได้ตั้งเป้า → เป้าหมาย 0%")
    void compareWithoutTarget() {
        Asset nvda = asset(3, "NVDA");
        when(holdingRepository.findByPortfolioId(9L)).thenReturn(List.of(holding(nvda, "1")));
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(3L)).thenReturn(Optional.of(close("2000000")));
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of());

        List<AllocationComparison> result = allocationService.compare(9L, "target");

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.currentPercent()).isEqualByComparingTo("100");
            assertThat(r.targetPercent()).isEqualByComparingTo("0");
        });
    }

    @Test
    @DisplayName("upsertTarget: มีเป้าเดิมอยู่แล้ว → แก้ค่าในแถวเดิม")
    void upsertUpdatesExisting() {
        AllocationTarget existing = AllocationTarget.builder().id(4L).targetPercent(new BigDecimal("10")).build();
        when(allocationTargetRepository.findByPortfolioIdAndAssetId(9L, 1L)).thenReturn(Optional.of(existing));
        when(allocationTargetRepository.save(existing)).thenReturn(existing);

        AllocationTarget result = allocationService.upsertTarget(9L, 1L, new BigDecimal("35"));

        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getTargetPercent()).isEqualByComparingTo("35");
        verify(portfolioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("upsertTarget: ยังไม่มีเป้า → สร้างแถวใหม่ผูกกับพอร์ตและสินทรัพย์")
    void upsertCreatesNew() {
        Portfolio portfolio = Portfolio.builder().id(9L).build();
        Asset aapl = asset(1, "AAPL");
        when(allocationTargetRepository.findByPortfolioIdAndAssetId(9L, 1L)).thenReturn(Optional.empty());
        when(portfolioRepository.findById(9L)).thenReturn(Optional.of(portfolio));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(aapl));
        when(allocationTargetRepository.save(any(AllocationTarget.class))).thenAnswer(inv -> inv.getArgument(0));

        AllocationTarget result = allocationService.upsertTarget(9L, 1L, new BigDecimal("40"));

        assertThat(result.getPortfolio()).isSameAs(portfolio);
        assertThat(result.getAsset()).isSameAs(aapl);
        assertThat(result.getTargetPercent()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("upsertTarget: สินทรัพย์ไม่มีอยู่ → ResourceNotFoundException")
    void upsertUnknownAsset() {
        when(allocationTargetRepository.findByPortfolioIdAndAssetId(9L, 99L)).thenReturn(Optional.empty());
        when(portfolioRepository.findById(9L)).thenReturn(Optional.of(Portfolio.builder().id(9L).build()));
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allocationService.upsertTarget(9L, 99L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(allocationTargetRepository, never()).save(any());
    }

    private static AllocationTarget target(Asset asset, String percent) {
        return AllocationTarget.builder().asset(asset).targetPercent(new BigDecimal(percent)).build();
    }

    @Test
    @DisplayName("upsertTargets: เป้าเดิม 60 + 40 แล้วแก้ทั้งสองตัวพร้อมกันเป็น 40 + 60 → รวม 100 บันทึกได้")
    void upsertTargetsMovesWeightBetweenAssets() {
        Asset aapl = asset(1, "AAPL");
        Asset msft = asset(2, "MSFT");
        AllocationTarget a = target(aapl, "60");
        AllocationTarget m = target(msft, "40");
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of(a, m));
        when(allocationTargetRepository.findByPortfolioIdAndAssetId(9L, 1L)).thenReturn(Optional.of(a));
        when(allocationTargetRepository.findByPortfolioIdAndAssetId(9L, 2L)).thenReturn(Optional.of(m));
        when(allocationTargetRepository.save(any(AllocationTarget.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<Long, BigDecimal> changes = new LinkedHashMap<>();
        changes.put(1L, new BigDecimal("40"));
        changes.put(2L, new BigDecimal("60"));

        List<AllocationTarget> saved = allocationService.upsertTargets(9L, changes);

        assertThat(saved).hasSize(2);
        assertThat(a.getTargetPercent()).isEqualByComparingTo("40");
        assertThat(m.getTargetPercent()).isEqualByComparingTo("60");
    }

    @Test
    @DisplayName("upsertTargets: ทีละตัวไม่เกิน 100 แต่รวมกัน 80 + 60 + 99 = 239% → IllegalArgumentException (400) และไม่บันทึกเลย")
    void upsertTargetsRejectsTotalOver100() {
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of());
        Map<Long, BigDecimal> changes = new LinkedHashMap<>();
        changes.put(1L, new BigDecimal("80"));
        changes.put(2L, new BigDecimal("60"));
        changes.put(3L, new BigDecimal("99"));

        assertThatThrownBy(() -> allocationService.upsertTargets(9L, changes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("239");
        verify(allocationTargetRepository, never()).save(any());
    }

    @Test
    @DisplayName("upsertTarget ทีละตัว: เป้าตัวอื่นรวม 90% แล้วตั้งตัวใหม่ 20% → รวม 110% ถูกปฏิเสธ (นับตัวที่ขายหมดแล้วด้วย)")
    void upsertSingleCountsOtherTargets() {
        when(allocationTargetRepository.findByPortfolioId(9L)).thenReturn(List.of(
                target(asset(1, "AAPL"), "70"), target(asset(2, "OLD"), "20")));

        assertThatThrownBy(() -> allocationService.upsertTarget(9L, 3L, new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("110");
        verify(allocationTargetRepository, never()).save(any());
    }
}
