package com.example.portfolio.service.impl;

import com.example.portfolio.service.ExchangeRateService;
import com.example.portfolio.service.market.ExchangeRate;
import com.example.portfolio.service.market.HistoricalPriceSource;
import com.example.portfolio.service.market.PriceBar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

// ดึงอัตรา USD/THB จาก Yahoo ผ่าน Adapter ตัวเดิม (HistoricalPriceSource) ด้วย ticker "THB=X"
// ไม่ต้องเขียน Adapter ใหม่: อัตราแลกเปลี่ยนมาในรูปราคาปิดรายวันเหมือนหุ้น ใช้ราคาปิดวันล่าสุด
// จำค่าไว้ตามเวลาที่ตั้ง (ค่าเริ่มต้น 60 นาที) ถ้าดึงรอบใหม่ไม่สำเร็จ ใช้ค่าล่าสุดที่เคยได้ต่อไป
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    static final String USD_THB_TICKER = "THB=X";

    private final HistoricalPriceSource priceSource;
    private final Duration cacheDuration;

    private volatile ExchangeRate cached;
    private volatile Instant fetchedAt = Instant.EPOCH;

    public ExchangeRateServiceImpl(HistoricalPriceSource priceSource,
                                   @Value("${market-data.fx.cache-minutes:60}") long cacheMinutes) {
        this.priceSource = priceSource;
        this.cacheDuration = Duration.ofMinutes(cacheMinutes);
    }

    @Override
    public Optional<ExchangeRate> usdToThb() {
        // หมดอายุเมื่อเวลาปัจจุบันถึงหรือเลยเวลาที่กำหนด (ใช้ !isBefore เพื่อให้ cache 0 นาที = ดึงใหม่ทุกครั้งเสมอ)
        if (cached == null || !Instant.now().isBefore(fetchedAt.plus(cacheDuration))) {
            refresh();
        }
        return Optional.ofNullable(cached);
    }

    private synchronized void refresh() {
        latestRate(priceSource.fetchDailyHistory(USD_THB_TICKER)).ifPresent(rate -> {
            cached = rate;
            fetchedAt = Instant.now();
        });
    }

    // ราคาปิดของวันล่าสุดที่มากกว่า 0 (list เรียงจากเก่าไปใหม่)
    private static Optional<ExchangeRate> latestRate(List<PriceBar> bars) {
        for (int i = bars.size() - 1; i >= 0; i--) {
            PriceBar bar = bars.get(i);
            if (bar.close() != null && bar.close().compareTo(BigDecimal.ZERO) > 0) {
                return Optional.of(new ExchangeRate("USD", "THB", bar.close(), bar.date()));
            }
        }
        return Optional.empty();
    }
}
