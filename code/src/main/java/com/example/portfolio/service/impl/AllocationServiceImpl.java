package com.example.portfolio.service.impl;

import com.example.portfolio.common.StrategyRegistry;
import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AllocationTargetRepository;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.AllocationService;
import com.example.portfolio.service.allocation.AllocationComparison;
import com.example.portfolio.service.allocation.AllocationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// เลือก AllocationStrategy จากชื่อที่ผู้ใช้ส่งมา (ผ่าน StrategyRegistry) แล้วเทียบผลกับ allocation_targets
// controller จึงไม่ต้องรู้จักคลาส Strategy ตัวจริงเลย (DIP + OCP)
@Service
public class AllocationServiceImpl implements AllocationService {

    private final HoldingRepository holdingRepository;
    private final AllocationTargetRepository allocationTargetRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final StrategyRegistry<AllocationStrategy> strategies;

    // Spring ฉีด AllocationStrategy ทุกตัวที่เป็น @Component มาเป็น List
    public AllocationServiceImpl(HoldingRepository holdingRepository,
                                 AllocationTargetRepository allocationTargetRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 PortfolioRepository portfolioRepository,
                                 AssetRepository assetRepository,
                                 List<AllocationStrategy> strategies) {
        this.holdingRepository = holdingRepository;
        this.allocationTargetRepository = allocationTargetRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.assetRepository = assetRepository;
        this.strategies = new StrategyRegistry<>("คำนวณสัดส่วน", strategies);
    }

    @Override
    public List<AllocationComparison> compare(Long portfolioId, String method) {
        AllocationStrategy strategy = strategies.get(method);
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolioId);
        List<AllocationTarget> targets = allocationTargetRepository.findByPortfolioId(portfolioId);

        Map<Long, BigDecimal> latestPrices = new HashMap<>();
        for (Holding h : holdings) {
            priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(h.getAsset().getId())
                    .ifPresent(ph -> latestPrices.put(h.getAsset().getId(), ph.getClose()));
        }

        Map<Long, BigDecimal> currentAllocation = strategy.calculateCurrentAllocation(holdings, latestPrices);
        Map<Long, BigDecimal> targetAllocation = new HashMap<>();
        for (AllocationTarget t : targets) {
            targetAllocation.put(t.getAsset().getId(), t.getTargetPercent());
        }

        List<AllocationComparison> result = new ArrayList<>();
        for (Holding h : holdings) {
            Long assetId = h.getAsset().getId();
            BigDecimal current = currentAllocation.getOrDefault(assetId, BigDecimal.ZERO);
            BigDecimal target = targetAllocation.getOrDefault(assetId, BigDecimal.ZERO);
            result.add(new AllocationComparison(
                    assetId, h.getAsset().getSymbol(), current, target, current.subtract(target)));
        }
        return result;
    }

    @Override
    public List<AllocationTarget> getTargets(Long portfolioId) {
        return allocationTargetRepository.findByPortfolioId(portfolioId);
    }

    @Override
    @Transactional
    public AllocationTarget upsertTarget(Long portfolioId, Long assetId, BigDecimal targetPercent) {
        AllocationTarget target = allocationTargetRepository.findByPortfolioIdAndAssetId(portfolioId, assetId)
                .orElseGet(() -> {
                    Portfolio portfolio = portfolioRepository.findById(portfolioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
                    Asset asset = assetRepository.findById(assetId)
                            .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
                    return AllocationTarget.builder().portfolio(portfolio).asset(asset).build();
                });
        target.setTargetPercent(targetPercent);
        return allocationTargetRepository.save(target);
    }
}
