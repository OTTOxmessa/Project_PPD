package com.example.portfolio.service.rebalance;

import com.example.portfolio.config.RebalanceValidationConfig;
import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

// ทดสอบ Command pattern (Buy/SellCommand) และ Chain of Responsibility (RebalanceValidationHandler)
@ExtendWith(MockitoExtension.class)
class CommandAndChainTest {

    @Mock
    private TransactionService transactionService;

    @Test
    @DisplayName("BuyCommand.execute() → บันทึกธุรกรรม BUY ผ่าน TransactionService")
    void buyCommand() {
        RebalanceCommand command = new BuyCommand(transactionService, 5L, 1L, new BigDecimal("3"), new BigDecimal("40"));

        command.execute();

        verify(transactionService).record(5L, 1L, TransactionType.BUY, new BigDecimal("3"), new BigDecimal("40"), null);
    }

    @Test
    @DisplayName("SellCommand.execute() → บันทึกธุรกรรม SELL ผ่าน TransactionService")
    void sellCommand() {
        RebalanceCommand command = new SellCommand(transactionService, 5L, 2L, new BigDecimal("1.5"), new BigDecimal("99"));

        command.execute();

        verify(transactionService).record(5L, 2L, TransactionType.SELL, new BigDecimal("1.5"), new BigDecimal("99"), null);
    }

    @Test
    @DisplayName("Chain: handler แรกผ่าน → ส่งต่อให้ handler ถัดไปตรวจ")
    void chainPassesToNextHandler() {
        List<String> visited = new ArrayList<>();
        RebalanceValidationHandler first = new MinimumHoldingsValidationHandler();
        first.setNext(new RebalanceValidationHandler() {
            @Override
            protected void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
                visited.add("second");
            }
        });
        Holding holding = Holding.builder().asset(Asset.builder().id(1L).build())
                .quantity(BigDecimal.ONE).avgCost(BigDecimal.ONE).build();
        Portfolio portfolio = Portfolio.builder().holdings(new ArrayList<>(List.of(holding))).build();

        first.validate(portfolio, List.of());

        assertThat(visited).containsExactly("second");
    }

    @Test
    @DisplayName("Chain: handler แรกไม่ผ่าน → หยุดทันที handler ถัดไปไม่ถูกเรียก")
    void chainStopsOnFailure() {
        List<String> visited = new ArrayList<>();
        RebalanceValidationHandler first = new MinimumHoldingsValidationHandler();
        first.setNext(new RebalanceValidationHandler() {
            @Override
            protected void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
                visited.add("second");
            }
        });

        assertThatThrownBy(() -> first.validate(Portfolio.builder().build(), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(visited).isEmpty();
    }

    @Test
    @DisplayName("RebalanceCommands: สร้าง Command ตามประเภทคำสั่งจากตาราง (ไม่ใช้ switch)")
    void factoryCreatesCommandByType() {
        TradeOrder sell = new TradeOrder(2L, "B", TransactionType.SELL, new BigDecimal("1.5"), new BigDecimal("99"));

        RebalanceCommand command = RebalanceCommands.forOrder(sell, 5L, transactionService);

        assertThat(command).isInstanceOf(SellCommand.class);
        command.execute();
        verify(transactionService).record(5L, 2L, TransactionType.SELL, new BigDecimal("1.5"), new BigDecimal("99"), null);
    }

    @Test
    @DisplayName("RebalanceCommands: ประเภทที่ไม่มี Command (DIVIDEND) → IllegalArgumentException")
    void factoryRejectsUnsupportedType() {
        TradeOrder dividend = new TradeOrder(2L, "B", TransactionType.DIVIDEND, BigDecimal.ONE, BigDecimal.ONE);

        assertThatThrownBy(() -> RebalanceCommands.forOrder(dividend, 5L, transactionService))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TradeOrderFormatter: แปลงคำสั่งเป็น JSON สำหรับเก็บใน log")
    void formatterProducesJson() {
        String json = TradeOrderFormatter.toJson(List.of(
                new TradeOrder(1L, "A", TransactionType.SELL, new BigDecimal("2"), new BigDecimal("70"))));

        assertThat(json).isEqualTo("[{\"symbol\":\"A\",\"type\":\"SELL\",\"quantity\":2,\"price\":70}]");
    }

    private static Portfolio portfolio(String... targetPercents) {
        Holding holding = Holding.builder().asset(Asset.builder().id(1L).build())
                .quantity(BigDecimal.ONE).avgCost(BigDecimal.ONE).build();
        List<AllocationTarget> targets = new ArrayList<>();
        for (String p : targetPercents) {
            targets.add(AllocationTarget.builder().targetPercent(new BigDecimal(p)).build());
        }
        return Portfolio.builder().holdings(new ArrayList<>(List.of(holding))).allocationTargets(targets).build();
    }

    @Test
    @DisplayName("Chain เต็มสาย: มี holding + เป้าหมายรวม 100% → ผ่าน")
    void fullChainPasses() {
        RebalanceValidationHandler chain = new RebalanceValidationConfig().rebalanceValidationChain();

        chain.validate(portfolio("60", "40"), List.of());
    }

    @Test
    @DisplayName("Chain เต็มสาย: ไม่มีเป้าหมาย → AllocationTargetsDefinedHandler ปฏิเสธ")
    void chainRejectsMissingTargets() {
        RebalanceValidationHandler chain = new RebalanceValidationConfig().rebalanceValidationChain();

        assertThatThrownBy(() -> chain.validate(portfolio(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("เป้าหมาย");
    }

    @Test
    @DisplayName("Chain เต็มสาย: เป้าหมายรวม 99.5% → TargetSumValidationHandler ปฏิเสธ แต่ 99.995% ยอมรับ (ปัดเศษ)")
    void chainChecksTargetSum() {
        RebalanceValidationHandler chain = new RebalanceValidationConfig().rebalanceValidationChain();

        assertThatThrownBy(() -> chain.validate(portfolio("60", "39.5"), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99.5");
        chain.validate(portfolio("33.33", "33.33", "33.335"), List.of());
    }
}
