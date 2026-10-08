package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.QuoteService;
import com.example.portfolio.service.market.Quote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

// จุดเดียวที่คำนวณ "ราคาล่าสุด" และ "% เปลี่ยนแปลงรายวัน" — ใช้ร่วมกันใน Watchlist, กระดานเทรด, Holdings, สรุปพอร์ต
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    public Quote getQuote(Asset asset) {
        List<PriceHistory> latestTwo = priceHistoryRepository.findTop2ByAssetIdOrderByPriceDateDesc(asset.getId());
        if (latestTwo.isEmpty()) {
            return new Quote(asset.getId(), asset.getSymbol(), asset.getName(), asset.getAssetType(),
                    asset.getExchange(), null, null, null, null, null, sourceOf(asset));
        }
        PriceHistory latest = latestTwo.get(0);
        BigDecimal price = latest.getClose();
        BigDecimal previous = latestTwo.size() > 1 ? latestTwo.get(1).getClose() : null;
        BigDecimal change = previous != null ? price.subtract(previous) : null;
        BigDecimal changePercent = (previous != null && previous.signum() != 0)
                ? change.multiply(HUNDRED).divide(previous, 2, RoundingMode.HALF_UP)
                : null;
        return new Quote(asset.getId(), asset.getSymbol(), asset.getName(), asset.getAssetType(),
                asset.getExchange(), price, previous, change, changePercent, latest.getPriceDate(), sourceOf(asset));
    }

    @Override
    public Optional<BigDecimal> latestPrice(Long assetId) {
        return priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(assetId).map(PriceHistory::getClose);
    }

    private static String sourceOf(Asset asset) {
        return asset.getPriceSource() == null ? null : asset.getPriceSource().name();
    }
}
