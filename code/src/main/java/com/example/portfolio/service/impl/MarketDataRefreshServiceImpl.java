package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.MarketIndexRepository;
import com.example.portfolio.service.MarketDataRefreshService;
import com.example.portfolio.service.PriceHistoryService;
import com.example.portfolio.service.market.RefreshSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

// วนอัปเดตราคาจริงทั้งระบบ และเว้นจังหวะระหว่างคำขอ — การนำเข้าราคาแต่ละตัวเป็นหน้าที่ของ PriceHistoryService (SRP)
@Service
public class MarketDataRefreshServiceImpl implements MarketDataRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshServiceImpl.class);

    private final AssetRepository assetRepository;
    private final MarketIndexRepository marketIndexRepository;
    private final PriceHistoryService priceHistoryService;
    private final long pauseBetweenRequestsMs;

    // เว้นจังหวะระหว่างคำขอ ลดโอกาสโดน Yahoo จำกัด (HTTP 429) — ปรับได้ที่ market-data.pause-ms
    public MarketDataRefreshServiceImpl(AssetRepository assetRepository,
                                        MarketIndexRepository marketIndexRepository,
                                        PriceHistoryService priceHistoryService,
                                        @Value("${market-data.pause-ms:400}") long pauseBetweenRequestsMs) {
        this.assetRepository = assetRepository;
        this.marketIndexRepository = marketIndexRepository;
        this.priceHistoryService = priceHistoryService;
        this.pauseBetweenRequestsMs = pauseBetweenRequestsMs;
    }

    // สินทรัพย์/ดัชนีตัวหนึ่งดึงไม่สำเร็จ ต้องไม่ทำให้ตัวอื่นไม่ถูกอัปเดต
    @Override
    public RefreshSummary refreshAll() {
        List<Asset> assets = assetRepository.findAll();
        int assetsUpdated = 0;
        for (Asset asset : assets) {
            try {
                if (priceHistoryService.importRealHistory(asset)) {
                    assetsUpdated++;
                }
            } catch (Exception e) {
                log.warn("อัปเดตราคา {} ไม่สำเร็จ: {}", asset.getSymbol(), e.getMessage());
            }
            pause();
        }
        List<MarketIndex> indices = marketIndexRepository.findAll();
        int indicesUpdated = 0;
        for (MarketIndex index : indices) {
            try {
                if (priceHistoryService.importRealIndexHistory(index)) {
                    indicesUpdated++;
                }
            } catch (Exception e) {
                log.warn("อัปเดตดัชนี {} ไม่สำเร็จ: {}", index.getIndexCode(), e.getMessage());
            }
            pause();
        }
        return new RefreshSummary(assetsUpdated, assets.size(), indicesUpdated, indices.size());
    }

    private void pause() {
        if (pauseBetweenRequestsMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseBetweenRequestsMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
