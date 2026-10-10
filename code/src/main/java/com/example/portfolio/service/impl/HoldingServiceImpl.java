package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.service.HoldingService;
import com.example.portfolio.service.holding.HoldingUpdateRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// หา/สร้าง holding แล้วส่งให้ HoldingUpdateRule ที่ตรงกับประเภทธุรกรรมเป็นคนคำนวณ
@Service
public class HoldingServiceImpl implements HoldingService {

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final Map<TransactionType, HoldingUpdateRule> rules = new EnumMap<>(TransactionType.class);

    public HoldingServiceImpl(HoldingRepository holdingRepository,
                              PortfolioRepository portfolioRepository,
                              AssetRepository assetRepository,
                              List<HoldingUpdateRule> rules) {
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.assetRepository = assetRepository;
        for (HoldingUpdateRule rule : rules) {
            this.rules.put(rule.type(), rule);
        }
    }

    @Override
    public List<Holding> getOpenPositions(Long portfolioId) {
        return holdingRepository.findByPortfolioId(portfolioId).stream()
                .filter(h -> h.getQuantity().signum() > 0)
                .toList();
    }

    @Override
    @Transactional
    public Holding applyTransaction(Long portfolioId, Long assetId, TransactionType type,
                                     BigDecimal quantity, BigDecimal price) {
        HoldingUpdateRule rule = rules.get(type);
        if (rule == null) {
            throw new IllegalArgumentException("ธุรกรรมประเภท " + type + " ไม่กระทบจำนวนหน่วยที่ถือ");
        }
        Holding holding = holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId)
                .orElseGet(() -> createEmptyHolding(portfolioId, assetId));
        rule.apply(holding, quantity, price);
        return holdingRepository.save(holding);
    }

    @Override
    @Transactional
    public Holding recalculate(Long portfolioId, Long assetId, List<Transaction> history) {
        Holding holding = holdingRepository.findByPortfolioIdAndAssetId(portfolioId, assetId)
                .orElseGet(() -> createEmptyHolding(portfolioId, assetId));
        holding.setQuantity(BigDecimal.ZERO);
        holding.setAvgCost(BigDecimal.ZERO);
        for (Transaction transaction : history) {
            HoldingUpdateRule rule = rules.get(transaction.getType());
            if (rule == null) {
                continue; // ปันผล/ฝาก/ถอน ไม่กระทบจำนวนหน่วย
            }
            try {
                rule.apply(holding, transaction.getQuantity(), transaction.getPrice());
            } catch (IllegalStateException oversold) {
                throw new IllegalStateException("แก้ไขไม่ได้: รายการวันที่ " + transaction.getExecutedAt().toLocalDate()
                        + " จะกลายเป็นขายเกินจำนวนที่ถืออยู่ ณ ตอนนั้น");
            }
        }
        return holdingRepository.save(holding);
    }

    private Holding createEmptyHolding(Long portfolioId, Long assetId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        return Holding.builder()
                .portfolio(portfolio)
                .asset(asset)
                .quantity(BigDecimal.ZERO)
                .avgCost(BigDecimal.ZERO)
                .build();
    }
}
