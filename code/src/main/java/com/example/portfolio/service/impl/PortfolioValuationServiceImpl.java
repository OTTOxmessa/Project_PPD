package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.repository.HoldingRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.service.PortfolioSummary;
import com.example.portfolio.service.PortfolioValuationService;
import com.example.portfolio.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// คำนวณมูลค่าตลาด / ต้นทุน / กำไรของแต่ละพอร์ต — แยกจาก PortfolioService (CRUD) ตาม SRP
@Service
@RequiredArgsConstructor
public class PortfolioValuationServiceImpl implements PortfolioValuationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final QuoteService quoteService;

    @Override
    public List<PortfolioSummary> summarize(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private PortfolioSummary toSummary(Portfolio portfolio) {
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal costBasis = BigDecimal.ZERO;
        int count = 0;
        for (Holding h : holdingRepository.findByPortfolioId(portfolio.getId())) {
            if (h.getQuantity().signum() <= 0) {
                continue;
            }
            count++;
            BigDecimal price = quoteService.latestPrice(h.getAsset().getId()).orElse(h.getAvgCost());
            marketValue = marketValue.add(h.getQuantity().multiply(price));
            costBasis = costBasis.add(h.getQuantity().multiply(h.getAvgCost()));
        }
        BigDecimal gain = marketValue.subtract(costBasis);
        BigDecimal gainPercent = costBasis.signum() > 0
                ? gain.multiply(HUNDRED).divide(costBasis, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PortfolioSummary(portfolio.getId(), portfolio.getName(), portfolio.getBaseCurrency(),
                marketValue.setScale(2, RoundingMode.HALF_UP), costBasis.setScale(2, RoundingMode.HALF_UP),
                gain.setScale(2, RoundingMode.HALF_UP), gainPercent, count);
    }
}
