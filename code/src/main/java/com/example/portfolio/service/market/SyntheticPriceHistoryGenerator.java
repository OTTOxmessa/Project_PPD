package com.example.portfolio.service.market;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// สร้าง "ราคาย้อนหลังจำลอง" ให้สินทรัพย์ที่ดึงราคาจริงไม่ได้ เพื่อให้กราฟ/ตัวชี้วัด/รีบาลานซ์ยังใช้งานได้
// - anchorPrice: ถ้ามี ราคาปิดวันล่าสุดจะเท่ากับค่านี้พอดี (เช่น ราคาที่ผู้ใช้กรอกตอนซื้อ)
// - seed จาก symbol ทำให้หุ้นตัวเดิมได้กราฟรูปเดิมเสมอ
@Component
@RequiredArgsConstructor
public class SyntheticPriceHistoryGenerator implements PriceHistoryFallback {

    private static final int DAYS = 365;

    // ความผันผวนรายวันโดยประมาณของแต่ละประเภท (ตารางข้อมูล ไม่ใช่ switch)
    private static final Map<AssetType, Double> DAILY_VOLATILITY = new EnumMap<>(Map.of(
            AssetType.CRYPTO, 0.035,
            AssetType.STOCK, 0.018,
            AssetType.ETF, 0.010,
            AssetType.MUTUAL_FUND, 0.010,
            AssetType.BOND, 0.003,
            AssetType.CASH, 0.0));

    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional
    public void backfillIfMissing(Asset asset, BigDecimal anchorPrice) {
        if (priceHistoryRepository.existsByAssetId(asset.getId())) {
            return;
        }
        Random random = new Random(asset.getSymbol().hashCode());
        double volatility = DAILY_VOLATILITY.getOrDefault(asset.getAssetType(), 0.018);

        double lastClose = (anchorPrice != null && anchorPrice.signum() > 0)
                ? anchorPrice.doubleValue()
                : 10 + Math.floorMod(asset.getSymbol().hashCode(), 490);

        // เดินย้อนหลังจากราคาล่าสุด (random walk) เพื่อให้วันสุดท้ายตรงกับ anchor พอดี
        double[] closes = new double[DAYS];
        closes[DAYS - 1] = lastClose;
        for (int i = DAYS - 2; i >= 0; i--) {
            double dailyReturn = 0.0004 + random.nextGaussian() * volatility;
            closes[i] = Math.max(closes[i + 1] / (1 + dailyReturn), 0.01);
        }

        LocalDate start = LocalDate.now().minusDays(DAYS - 1);
        List<PriceHistory> rows = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) {
            double close = closes[i];
            double open = (i == 0) ? close : closes[i - 1] * (1 + random.nextGaussian() * volatility * 0.3);
            double high = Math.max(open, close) * (1 + Math.abs(random.nextGaussian()) * volatility * 0.5);
            double low = Math.min(open, close) * (1 - Math.abs(random.nextGaussian()) * volatility * 0.5);
            rows.add(PriceHistory.builder()
                    .asset(asset)
                    .priceDate(start.plusDays(i))
                    .open(toPrice(open))
                    .high(toPrice(high))
                    .low(toPrice(low))
                    .close(toPrice(close))
                    .volume(1_000_000L)
                    .build());
        }
        priceHistoryRepository.saveAll(rows);
    }

    private static BigDecimal toPrice(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
