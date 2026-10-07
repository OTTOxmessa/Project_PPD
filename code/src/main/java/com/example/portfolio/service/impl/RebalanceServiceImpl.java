package com.example.portfolio.service.impl;

import com.example.portfolio.common.StrategyRegistry;
import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.RebalanceLog;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AllocationTargetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.repository.RebalanceLogRepository;
import com.example.portfolio.service.RebalanceService;
import com.example.portfolio.service.TransactionService;
import com.example.portfolio.service.rebalance.RebalanceCommands;
import com.example.portfolio.service.rebalance.RebalanceStrategy;
import com.example.portfolio.service.rebalance.RebalanceValidationHandler;
import com.example.portfolio.service.rebalance.TradeOrder;
import com.example.portfolio.service.rebalance.TradeOrderFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ประสานงาน 3 pattern: Strategy (เลือกวิธีคำนวณ), Chain of Responsibility (ตรวจก่อนทำจริง), Command (ซื้อขายจริง)
// การสร้าง Command และการจัดรูป log แยกไปอยู่ที่ RebalanceCommands / TradeOrderFormatter (SRP)
@Service
public class RebalanceServiceImpl implements RebalanceService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AllocationTargetRepository allocationTargetRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final RebalanceLogRepository rebalanceLogRepository;
    private final TransactionService transactionService;
    private final StrategyRegistry<RebalanceStrategy> strategies;
    private final RebalanceValidationHandler validationChain;

    public RebalanceServiceImpl(PortfolioRepository portfolioRepository,
                                HoldingRepository holdingRepository,
                                AllocationTargetRepository allocationTargetRepository,
                                PriceHistoryRepository priceHistoryRepository,
                                RebalanceLogRepository rebalanceLogRepository,
                                TransactionService transactionService,
                                List<RebalanceStrategy> strategies,
                                RebalanceValidationHandler validationChain) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.allocationTargetRepository = allocationTargetRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.rebalanceLogRepository = rebalanceLogRepository;
        this.transactionService = transactionService;
        this.strategies = new StrategyRegistry<>("รีบาลานซ์", strategies);
        this.validationChain = validationChain;
    }

    @Override
    public List<TradeOrder> preview(Long portfolioId, String method) {
        return computeTrades(portfolioId, strategies.get(method));
    }

    @Override
    @Transactional
    public RebalanceLog execute(Long portfolioId, String method) {
        RebalanceStrategy strategy = strategies.get(method);
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        List<TradeOrder> trades = computeTrades(portfolioId, strategy);

        // Chain of Responsibility: ต้องมี holding -> ต้องมีเป้าหมาย -> เป้าหมายรวม 100% (ประกอบที่ RebalanceValidationConfig)
        validationChain.validate(portfolio, trades);

        // ขายก่อนซื้อ (SELL ทิศ -1 มาก่อน BUY ทิศ +1) — ได้เงินจากการขายมาใช้ซื้อ เหมือนการรีบาลานซ์จริง
        List<TradeOrder> ordered = trades.stream()
                .sorted(Comparator.comparingInt(t -> t.type().holdingDirection()))
                .toList();
        for (TradeOrder order : ordered) {
            RebalanceCommands.forOrder(order, portfolioId, transactionService).execute();
        }

        RebalanceLog log = RebalanceLog.builder()
                .portfolio(portfolio)
                .method(strategy.method())
                .details(TradeOrderFormatter.toJson(ordered))
                .build();
        return rebalanceLogRepository.save(log);
    }

    @Override
    public Page<RebalanceLog> getHistory(Long portfolioId, Pageable pageable) {
        return rebalanceLogRepository.findByPortfolioId(portfolioId, pageable);
    }

    private List<TradeOrder> computeTrades(Long portfolioId, RebalanceStrategy strategy) {
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        List<AllocationTarget> targets = allocationTargetRepository.findByPortfolioId(portfolioId);
        return strategy.computeTrades(holdings, targets, latestPricesOf(holdings));
    }

    private Map<Long, BigDecimal> latestPricesOf(List<Holding> holdings) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        for (Holding h : holdings) {
            priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(h.getAsset().getId())
                    .ifPresent(ph -> prices.put(h.getAsset().getId(), ph.getClose()));
        }
        return prices;
    }
}
