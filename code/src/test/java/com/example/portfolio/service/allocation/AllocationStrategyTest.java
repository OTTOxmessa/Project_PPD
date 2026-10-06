package com.example.portfolio.service.allocation;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ทดสอบ Strategy pattern ของการคำนวณสัดส่วนสินทรัพย์ทั้ง 3 แบบ (ไม่ต้องใช้ Spring หรือฐานข้อมูล)
class AllocationStrategyTest {

    private static Holding holding(long assetId, AssetType type, String qty) {
        Asset asset = Asset.builder().id(assetId).symbol("A" + assetId).assetType(type).build();
        return Holding.builder().asset(asset).quantity(new BigDecimal(qty)).avgCost(BigDecimal.ZERO).build();
    }

    @Nested
    @DisplayName("TargetPercentageAllocationStrategy — ตามมูลค่าตลาดจริง")
    class TargetPercentage {
        private final AllocationStrategy strategy = new TargetPercentageAllocationStrategy();

        @Test
        @DisplayName("มูลค่า 300 กับ 100 → 75% กับ 25%")
        void splitsByMarketValue() {
            List<Holding> holdings = List.of(holding(1, AssetType.STOCK, "10"), holding(2, AssetType.STOCK, "5"));
            Map<Long, BigDecimal> prices = Map.of(1L, new BigDecimal("30"), 2L, new BigDecimal("20"));

            Map<Long, BigDecimal> result = strategy.calculateCurrentAllocation(holdings, prices);

            assertThat(result.get(1L)).isEqualByComparingTo("75");
            assertThat(result.get(2L)).isEqualByComparingTo("25");
        }

        @Test
        @DisplayName("ไม่มีราคาเลย (มูลค่ารวม 0) → คืนผลว่าง ไม่หารด้วยศูนย์")
        void zeroTotalValue() {
            Map<Long, BigDecimal> result = strategy.calculateCurrentAllocation(
                    List.of(holding(1, AssetType.STOCK, "10")), Map.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("EqualWeightAllocationStrategy — น้ำหนักเท่ากันทุกตัว")
    class EqualWeight {
        private final AllocationStrategy strategy = new EqualWeightAllocationStrategy();

        @Test
        @DisplayName("3 สินทรัพย์ → ตัวละ 33.3333%")
        void equalShares() {
            List<Holding> holdings = List.of(
                    holding(1, AssetType.STOCK, "1"), holding(2, AssetType.BOND, "500"), holding(3, AssetType.CRYPTO, "0.01"));

            Map<Long, BigDecimal> result = strategy.calculateCurrentAllocation(holdings, Map.of());

            assertThat(result).hasSize(3);
            assertThat(result.values()).allSatisfy(p -> assertThat(p).isEqualByComparingTo("33.3333"));
        }

        @Test
        @DisplayName("ไม่มี holding → คืนผลว่าง")
        void emptyPortfolio() {
            assertThat(strategy.calculateCurrentAllocation(List.of(), Map.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("RiskBasedAllocationStrategy — ถ่วงด้วยความเสี่ยงของประเภทสินทรัพย์")
    class RiskBased {
        private final AllocationStrategy strategy = new RiskBasedAllocationStrategy();

        @Test
        @DisplayName("หุ้นกับคริปโตมูลค่าเท่ากัน → คริปโตได้น้ำหนักมากกว่า (1.0 : 1.5 = 40% : 60%)")
        void cryptoWeighsMore() {
            List<Holding> holdings = List.of(holding(1, AssetType.STOCK, "10"), holding(2, AssetType.CRYPTO, "1"));
            Map<Long, BigDecimal> prices = Map.of(1L, new BigDecimal("10"), 2L, new BigDecimal("100"));

            Map<Long, BigDecimal> result = strategy.calculateCurrentAllocation(holdings, prices);

            assertThat(result.get(1L)).isEqualByComparingTo("40");
            assertThat(result.get(2L)).isEqualByComparingTo("60");
        }

        @Test
        @DisplayName("สัดส่วนทุกตัวรวมกันได้ 100%")
        void sumsToHundred() {
            List<Holding> holdings = List.of(
                    holding(1, AssetType.STOCK, "10"), holding(2, AssetType.BOND, "20"), holding(3, AssetType.CASH, "1000"));
            Map<Long, BigDecimal> prices = Map.of(1L, new BigDecimal("33"), 2L, new BigDecimal("101"), 3L, BigDecimal.ONE);

            BigDecimal sum = strategy.calculateCurrentAllocation(holdings, prices).values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(sum).isBetween(new BigDecimal("99.999"), new BigDecimal("100.001"));
        }
    }
}
