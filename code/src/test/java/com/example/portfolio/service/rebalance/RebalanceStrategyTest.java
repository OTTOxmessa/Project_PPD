package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// ทดสอบ Strategy การรีบาลานซ์ 2 แบบ
// พอร์ตตัวอย่าง: A 10 หน่วย @70 = 700, B 10 หน่วย @30 = 300 → มูลค่ารวม 1,000 (A 70%, B 30%)
class RebalanceStrategyTest {

    private static final Asset A = Asset.builder().id(1L).symbol("A").build();
    private static final Asset B = Asset.builder().id(2L).symbol("B").build();
    private static final Map<Long, BigDecimal> PRICES = Map.of(1L, new BigDecimal("70"), 2L, new BigDecimal("30"));

    private static List<Holding> holdings() {
        return List.of(
                Holding.builder().asset(A).quantity(new BigDecimal("10")).avgCost(BigDecimal.ZERO).build(),
                Holding.builder().asset(B).quantity(new BigDecimal("10")).avgCost(BigDecimal.ZERO).build());
    }

    private static List<AllocationTarget> targets(String a, String b) {
        return List.of(
                AllocationTarget.builder().asset(A).targetPercent(new BigDecimal(a)).build(),
                AllocationTarget.builder().asset(B).targetPercent(new BigDecimal(b)).build());
    }

    private static TradeOrder orderFor(List<TradeOrder> orders, String symbol) {
        return orders.stream().filter(o -> o.symbol().equals(symbol)).findFirst().orElseThrow();
    }

    @Nested
    @DisplayName("ThresholdRebalanceStrategy — ทำเฉพาะตัวที่เบี่ยงเกิน 5%")
    class Threshold {
        private final RebalanceStrategy strategy = new ThresholdRebalanceStrategy();

        @Test
        @DisplayName("เป้า 50/50 (เบี่ยง 20%) → ขาย A 2.857143 หน่วย ซื้อ B 6.666667 หน่วย")
        void driftAboveThreshold() {
            List<TradeOrder> orders = strategy.computeTrades(holdings(), targets("50", "50"), PRICES);

            assertThat(orders).hasSize(2);
            TradeOrder sellA = orderFor(orders, "A");
            assertThat(sellA.type()).isEqualTo(TransactionType.SELL);
            assertThat(sellA.quantity()).isEqualByComparingTo("2.857143");
            assertThat(sellA.estimatedPrice()).isEqualByComparingTo("70");
            TradeOrder buyB = orderFor(orders, "B");
            assertThat(buyB.type()).isEqualTo(TransactionType.BUY);
            assertThat(buyB.quantity()).isEqualByComparingTo("6.666667");
        }

        @Test
        @DisplayName("เป้า 68/32 (เบี่ยงแค่ 2%) → ไม่ต้องซื้อขาย")
        void driftWithinThreshold() {
            assertThat(strategy.computeTrades(holdings(), targets("68", "32"), PRICES)).isEmpty();
        }

        @Test
        @DisplayName("ไม่มีราคาเลย → ไม่มีคำสั่ง (ไม่หารด้วยศูนย์)")
        void noPrices() {
            assertThat(strategy.computeTrades(holdings(), targets("50", "50"), Map.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("CalendarRebalanceStrategy — กลับไปตามเป้าเต็มจำนวนทุกครั้ง")
    class Calendar {
        private final RebalanceStrategy strategy = new CalendarRebalanceStrategy();

        @Test
        @DisplayName("เป้า 68/32 แม้เบี่ยงน้อย ก็ยังสั่งขาย A และซื้อ B")
        void rebalancesSmallDrift() {
            List<TradeOrder> orders = strategy.computeTrades(holdings(), targets("68", "32"), PRICES);

            assertThat(orderFor(orders, "A").type()).isEqualTo(TransactionType.SELL);
            assertThat(orderFor(orders, "A").quantity()).isEqualByComparingTo("0.285714");
            assertThat(orderFor(orders, "B").type()).isEqualTo(TransactionType.BUY);
            assertThat(orderFor(orders, "B").quantity()).isEqualByComparingTo("0.666667");
        }

        @Test
        @DisplayName("สัดส่วนตรงเป้าพอดี (70/30) → ไม่มีคำสั่ง")
        void alreadyOnTarget() {
            assertThat(strategy.computeTrades(holdings(), targets("70", "30"), PRICES)).isEmpty();
        }

        @Test
        @DisplayName("สินทรัพย์ที่ไม่มีราคาถูกข้าม")
        void skipsAssetWithoutPrice() {
            List<TradeOrder> orders = strategy.computeTrades(holdings(), targets("50", "50"),
                    Map.of(1L, new BigDecimal("70")));

            assertThat(orders).extracting(TradeOrder::symbol).containsExactly("A");
        }
    }
}
