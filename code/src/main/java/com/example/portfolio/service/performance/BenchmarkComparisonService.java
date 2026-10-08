package com.example.portfolio.service.performance;

import com.example.portfolio.domain.entity.IndexPriceHistory;
import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.IndexPriceHistoryRepository;
import com.example.portfolio.repository.MarketIndexRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Concrete class ของ Template Method — implement แค่ 2 hook: มูลค่าพอร์ต และมูลค่า benchmark ณ วันที่กำหนด
@Service
@RequiredArgsConstructor
public class BenchmarkComparisonService extends PerformanceReportTemplate {

    // จุดเริ่มต้นสำหรับ query transaction ทั้งหมดจนถึงวันที่กำหนด
    // (ไม่ใช้ LocalDateTime.MIN เพราะเกินช่วงที่ PostgreSQL timestamp รองรับ)
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final TransactionRepository transactionRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MarketIndexRepository marketIndexRepository;
    private final IndexPriceHistoryRepository indexPriceHistoryRepository;

    @Override
    protected BigDecimal getPortfolioValue(Long portfolioId, LocalDate date) {
        // มูลค่า ณ วันที่ date = sum(จำนวนหน่วยสะสมถึงวันนั้น x ราคาปิดล่าสุด ณ หรือก่อนวันนั้น)
        List<Transaction> transactions = transactionRepository
                .findByPortfolioIdAndExecutedAtBetween(portfolioId, EPOCH, date.atTime(23, 59, 59));

        Map<Long, BigDecimal> netQuantityByAsset = new HashMap<>();
        for (Transaction t : transactions) {
            BigDecimal signedQty = t.getType().signedQuantity(t.getQuantity()); // BUY +, SELL -, อื่น ๆ 0
            netQuantityByAsset.merge(t.getAsset().getId(), signedQty, BigDecimal::add);
        }

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : netQuantityByAsset.entrySet()) {
            BigDecimal closePrice = priceHistoryRepository
                    .findTopByAssetIdAndPriceDateLessThanEqualOrderByPriceDateDesc(entry.getKey(), date)
                    .map(PriceHistory::getClose)
                    .orElse(BigDecimal.ZERO);
            totalValue = totalValue.add(entry.getValue().multiply(closePrice));
        }
        return totalValue;
    }

    @Override
    protected BigDecimal getBenchmarkValue(String benchmarkCode, LocalDate date) {
        MarketIndex index = marketIndexRepository.findByIndexCode(benchmarkCode)
                .orElseThrow(() -> new ResourceNotFoundException("Market index not found: " + benchmarkCode));

        return indexPriceHistoryRepository
                .findTopByMarketIndexIdAndPriceDateLessThanEqualOrderByPriceDateDesc(index.getId(), date)
                .map(IndexPriceHistory::getCloseValue)
                .orElse(BigDecimal.ZERO);
    }
}
