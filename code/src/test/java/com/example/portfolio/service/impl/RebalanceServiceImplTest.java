package com.example.portfolio.service.impl;

import com.example.portfolio.config.RebalanceValidationConfig;
import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.RebalanceLog;
import com.example.portfolio.domain.enums.RebalanceMethod;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AllocationTargetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.repository.RebalanceLogRepository;
import com.example.portfolio.service.TransactionService;
import com.example.portfolio.service.rebalance.RebalanceStrategy;
import com.example.portfolio.service.rebalance.TradeOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบ flow รีบาลานซ์จริง: เลือก Strategy จากชื่อ → Chain of Responsibility → Command (ขายก่อนซื้อ) → บันทึก RebalanceLog
@ExtendWith(MockitoExtension.class)
class RebalanceServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AllocationTargetRepository allocationTargetRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private RebalanceLogRepository rebalanceLogRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private RebalanceStrategy strategy;

    private RebalanceServiceImpl rebalanceService;

    private static final TradeOrder BUY_B =
            new TradeOrder(2L, "B", TransactionType.BUY, new BigDecimal("6.666667"), new BigDecimal("30"));
    private static final TradeOrder SELL_A =
            new TradeOrder(1L, "A", TransactionType.SELL, new BigDecimal("2.857143"), new BigDecimal("70"));

    @BeforeEach
    void setUp() {
        when(strategy.key()).thenReturn("threshold");
        rebalanceService = new RebalanceServiceImpl(portfolioRepository, holdingRepository, allocationTargetRepository,
                priceHistoryRepository, rebalanceLogRepository, transactionService, List.of(strategy),
                new RebalanceValidationConfig().rebalanceValidationChain());
    }

    private Portfolio portfolioWithHoldings() {
        return portfolioWithTargets("50", "50");
    }

    // พอร์ตที่มี holding 1 ตัว และเป้าหมาย 2 ตัวตามที่ระบุ (เพื่อผ่าน/ไม่ผ่าน chain ตรวจสอบ)
    private Portfolio portfolioWithTargets(String... percents) {
        Holding holding = Holding.builder().asset(Asset.builder().id(1L).symbol("A").build())
                .quantity(BigDecimal.TEN).avgCost(BigDecimal.ONE).build();
        List<AllocationTarget> targets = new ArrayList<>();
        for (int i = 0; i < percents.length; i++) {
            targets.add(AllocationTarget.builder()
                    .asset(Asset.builder().id((long) i + 1).build())
                    .targetPercent(new BigDecimal(percents[i])).build());
        }
        return Portfolio.builder().id(5L)
                .holdings(new ArrayList<>(List.of(holding)))
                .allocationTargets(targets)
                .build();
    }

    private void stubTradesFor(Portfolio portfolio) {
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioId(5L)).thenReturn(portfolio.getHoldings());
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(1L)).thenReturn(Optional.empty());
        when(allocationTargetRepository.findByPortfolioId(5L)).thenReturn(List.of());
        when(strategy.computeTrades(any(), any(), anyMap())).thenReturn(List.of(SELL_A));
    }

    @Test
    @DisplayName("preview: คืนคำสั่งจาก Strategy โดยยังไม่ซื้อขายจริง")
    void previewDoesNotTrade() {
        Portfolio portfolio = portfolioWithHoldings();
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioId(5L)).thenReturn(portfolio.getHoldings());
        when(allocationTargetRepository.findByPortfolioId(5L)).thenReturn(List.<AllocationTarget>of());
        when(strategy.computeTrades(any(), any(), anyMap())).thenReturn(List.of(BUY_B));

        assertThat(rebalanceService.preview(5L, "threshold")).containsExactly(BUY_B);
        verifyNoInteractions(transactionService);
        verify(rebalanceLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("preview: เป้าหมายรวม 90% → Chain ปฏิเสธตั้งแต่ตอนดูแผน ไม่ต้องรอกดยืนยัน")
    void previewRejectedWhenTargetsDoNotSumTo100() {
        stubTradesFor(portfolioWithTargets("60", "30"));

        assertThatThrownBy(() -> rebalanceService.preview(5L, "threshold"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("90");
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("preview: พอร์ตไม่มีอยู่ → ResourceNotFoundException")
    void previewUnknownPortfolio() {
        when(portfolioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rebalanceService.preview(5L, "threshold"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(strategy, never()).computeTrades(any(), any(), anyMap());
    }

    @Test
    @DisplayName("preview: ชื่อวิธีที่ไม่รู้จัก → IllegalArgumentException (400)")
    void previewUnknownMethod() {
        assertThatThrownBy(() -> rebalanceService.preview(5L, "yolo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold");
        verifyNoInteractions(holdingRepository);
    }

    @Test
    @DisplayName("execute: ขายก่อนซื้อเสมอ แม้ Strategy จะคืนคำสั่งซื้อมาก่อน แล้วบันทึก log ตามประเภทของ Strategy")
    void executeSellsBeforeBuying() {
        Portfolio portfolio = portfolioWithHoldings();
        when(strategy.method()).thenReturn(RebalanceMethod.THRESHOLD);
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioId(5L)).thenReturn(portfolio.getHoldings());
        when(priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(1L)).thenReturn(Optional.empty());
        when(allocationTargetRepository.findByPortfolioId(5L)).thenReturn(List.of());
        when(strategy.computeTrades(any(), any(), anyMap())).thenReturn(List.of(BUY_B, SELL_A));
        when(rebalanceLogRepository.save(any(RebalanceLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RebalanceLog log = rebalanceService.execute(5L, "threshold");

        InOrder order = inOrder(transactionService);
        order.verify(transactionService).record(eq(5L), eq(1L), eq(TransactionType.SELL),
                eq(new BigDecimal("2.857143")), eq(new BigDecimal("70")), isNull());
        order.verify(transactionService).record(eq(5L), eq(2L), eq(TransactionType.BUY),
                eq(new BigDecimal("6.666667")), eq(new BigDecimal("30")), isNull());
        assertThat(log.getMethod()).isEqualTo(RebalanceMethod.THRESHOLD);
        assertThat(log.getPortfolio()).isSameAs(portfolio);
        assertThat(log.getDetails()).startsWith("[{\"symbol\":\"A\",\"type\":\"SELL\"").contains("\"symbol\":\"B\"");
    }

    @Test
    @DisplayName("execute: พอร์ตไม่มี holding → Chain ปฏิเสธ (409) และไม่ซื้อขาย ไม่บันทึก log")
    void executeRejectedByValidationChain() {
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(Portfolio.builder().id(5L).build()));
        when(holdingRepository.findByPortfolioId(5L)).thenReturn(List.of());
        when(allocationTargetRepository.findByPortfolioId(5L)).thenReturn(List.of());
        when(strategy.computeTrades(any(), any(), anyMap())).thenReturn(List.of());

        assertThatThrownBy(() -> rebalanceService.execute(5L, "threshold"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(transactionService);
        verify(rebalanceLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("execute: พอร์ตไม่มีอยู่ → ResourceNotFoundException และไม่คำนวณแผน")
    void executeUnknownPortfolio() {
        when(portfolioRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rebalanceService.execute(5L, "threshold"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(strategy, never()).computeTrades(any(), any(), anyMap());
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("execute: ยังไม่ตั้งเป้าหมาย → Chain ห่วงที่ 2 ปฏิเสธ ไม่ขายทั้งพอร์ต")
    void executeRejectedWithoutTargets() {
        stubTradesFor(portfolioWithTargets());

        assertThatThrownBy(() -> rebalanceService.execute(5L, "threshold"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("เป้าหมาย");
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("execute: เป้าหมายรวมได้ 90% → Chain ห่วงที่ 3 ปฏิเสธ")
    void executeRejectedWhenTargetsDoNotSumTo100() {
        stubTradesFor(portfolioWithTargets("60", "30"));

        assertThatThrownBy(() -> rebalanceService.execute(5L, "threshold"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("90");
        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("getHistory: ส่ง Pageable ต่อให้ repository และคืนผลตามหน้า")
    void historyIsPaged() {
        PageRequest page = PageRequest.of(0, 10);
        RebalanceLog log = RebalanceLog.builder().id(1L).build();
        when(rebalanceLogRepository.findByPortfolioId(5L, page)).thenReturn(new PageImpl<>(List.of(log), page, 1));

        assertThat(rebalanceService.getHistory(5L, page).getContent()).containsExactly(log);
    }
}
