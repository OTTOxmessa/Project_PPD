package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.IndexPriceHistory;
import com.example.portfolio.domain.entity.MarketIndex;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.enums.PriceSource;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.IndexPriceHistoryRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.PriceHistoryService;
import com.example.portfolio.service.market.HistoricalPriceSource;
import com.example.portfolio.service.market.PriceBar;
import com.example.portfolio.service.market.PriceHistoryFallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// จุดเดียวที่ตัดสินใจว่าราคาย้อนหลังของสินทรัพย์มาจากไหน:
// 1) ลองแหล่งราคาจริง (HistoricalPriceSource = Yahoo) ก่อน
// 2) ไม่สำเร็จ -> ใช้ทางสำรอง (PriceHistoryFallback = ข้อมูลจำลอง) เพื่อให้ระบบยังใช้งานได้เสมอ
// ขึ้นกับ interface ทั้งสองฝั่ง จึงเปลี่ยนผู้ให้บริการหรือวิธีสำรองได้โดยไม่แก้คลาสนี้ (DIP)
@Service
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PriceHistoryServiceImpl.class);
    static final int MIN_INDEX_HISTORY_DAYS = 30;

    private final HistoricalPriceSource priceSource;
    private final PriceHistoryFallback fallback;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IndexPriceHistoryRepository indexPriceHistoryRepository;
    private final AssetRepository assetRepository;
    private final TransactionTemplate transactionTemplate;

    public PriceHistoryServiceImpl(HistoricalPriceSource priceSource,
                                   PriceHistoryFallback fallback,
                                   PriceHistoryRepository priceHistoryRepository,
                                   IndexPriceHistoryRepository indexPriceHistoryRepository,
                                   AssetRepository assetRepository,
                                   PlatformTransactionManager transactionManager) {
        this.priceSource = priceSource;
        this.fallback = fallback;
        this.priceHistoryRepository = priceHistoryRepository;
        this.indexPriceHistoryRepository = indexPriceHistoryRepository;
        this.assetRepository = assetRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void ensureHistory(Asset asset, BigDecimal anchorPrice) {
        if (priceHistoryRepository.existsByAssetId(asset.getId())) {
            return;
        }
        if (!importRealHistory(asset)) {
            transactionTemplate.executeWithoutResult(status -> {
                fallback.backfillIfMissing(asset, anchorPrice);
                asset.setPriceSource(PriceSource.SYNTHETIC);
                assetRepository.save(asset);
            });
        }
    }

    @Override
    public boolean importRealHistory(Asset asset) {
        Optional<String> ticker = priceSource.resolveTicker(asset);
        if (ticker.isEmpty()) {
            return false;
        }
        // เรียก HTTP นอก transaction — ไม่ถือ connection ฐานข้อมูลไว้ระหว่างรอ Yahoo
        List<PriceBar> bars = priceSource.fetchDailyHistory(ticker.get());
        if (bars.isEmpty()) {
            return false;
        }
        transactionTemplate.executeWithoutResult(status -> {
            priceHistoryRepository.purgeByAssetId(asset.getId());
            priceHistoryRepository.saveAll(bars.stream()
                    .map(b -> PriceHistory.builder()
                            .asset(asset)
                            .priceDate(b.date())
                            .open(b.open())
                            .high(b.high())
                            .low(b.low())
                            .close(b.close())
                            .volume(b.volume())
                            .build())
                    .toList());
            asset.setPriceSource(PriceSource.YAHOO);
            assetRepository.save(asset);
        });
        log.info("นำเข้าราคา {} จาก {} แล้ว {} วัน", asset.getSymbol(), priceSource.name(), bars.size());
        return true;
    }

    @Override
    public boolean importRealIndexHistory(MarketIndex index) {
        Optional<String> ticker = priceSource.resolveIndexTicker(index.getIndexCode());
        if (ticker.isEmpty()) {
            return false;
        }
        List<PriceBar> bars = priceSource.fetchDailyHistory(ticker.get());
        // ดัชนีใช้เป็นเกณฑ์เปรียบเทียบผลตอบแทน ถ้าแหล่งภายนอกส่งมาแค่ไม่กี่วัน (เช่น ได้มาแค่ 1 วัน)
        // ห้ามลบประวัติเดิมทิ้ง ไม่อย่างนั้นราคาต้นช่วงกับปลายช่วงจะเท่ากัน และผลตอบแทนตลาดกลายเป็น 0% ทุกช่วง
        if (bars.size() < MIN_INDEX_HISTORY_DAYS) {
            log.warn("ข้อมูลดัชนี {} จาก {} มีแค่ {} วัน (ต้องมีอย่างน้อย {}) จึงใช้ข้อมูลเดิม",
                    index.getIndexCode(), priceSource.name(), bars.size(), MIN_INDEX_HISTORY_DAYS);
            return false;
        }
        transactionTemplate.executeWithoutResult(status -> {
            indexPriceHistoryRepository.purgeByMarketIndexId(index.getId());
            indexPriceHistoryRepository.saveAll(bars.stream()
                    .map(b -> IndexPriceHistory.builder()
                            .marketIndex(index)
                            .priceDate(b.date())
                            .closeValue(b.close())
                            .build())
                    .toList());
        });
        log.info("นำเข้าดัชนี {} จาก {} แล้ว {} วัน", index.getIndexCode(), priceSource.name(), bars.size());
        return true;
    }
}
