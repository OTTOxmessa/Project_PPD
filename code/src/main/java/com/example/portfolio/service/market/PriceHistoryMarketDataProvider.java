package com.example.portfolio.service.market;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// อีก implementation ของ MarketDataProvider ที่อ่านราคาล่าสุดจากตาราง price_history
// @Primary = ให้ scheduler ใช้ตัวนี้เป็นค่าเริ่มต้น (ไม่ต้องพึ่ง API ภายนอก ใช้เดโมได้ทันที)
// พอเลือกผู้ให้บริการ API จริงได้แล้ว ย้าย @Primary ไปไว้ที่ ExternalMarketDataAdapter ได้เลย
// โดย scheduler/service ไม่ต้องแก้แม้แต่บรรทัดเดียว — ตัวอย่างของ DIP + OCP
@Component
@Primary
@RequiredArgsConstructor
public class PriceHistoryMarketDataProvider implements MarketDataProvider {

    private final AssetRepository assetRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    public BigDecimal getLatestPrice(String symbol) {
        Asset asset = assetRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + symbol));
        return priceHistoryRepository.findTopByAssetIdOrderByPriceDateDesc(asset.getId())
                .map(PriceHistory::getClose)
                .orElseThrow(() -> new IllegalStateException("ยังไม่มีข้อมูลราคาของ " + symbol));
    }
}
