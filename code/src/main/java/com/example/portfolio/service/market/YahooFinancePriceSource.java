package com.example.portfolio.service.market;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

// Adapter: แปลงข้อมูลจาก Yahoo Finance ให้อยู่ในรูป PriceBar ของระบบ
//
// ข้อควรรู้:
// - Yahoo ไม่มี API ทางการ ใช้ endpoint /v8/finance/chart ที่หน้าเว็บ Yahoo ใช้เอง (ไม่ต้องใช้ key/cookie)
//   endpoint นี้อาจเปลี่ยนหรือถูกจำกัดการใช้งาน (HTTP 429) ได้ทุกเมื่อ -> คืน list ว่าง แล้วระบบใช้ข้อมูลจำลองแทน
// - ข้อมูล Yahoo Finance ให้ใช้เพื่อการส่วนตัว/การศึกษาตามเงื่อนไขของ Yahoo
@Component
public class YahooFinancePriceSource implements HistoricalPriceSource {

    private static final Logger log = LoggerFactory.getLogger(YahooFinancePriceSource.class);
    private static final String CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=1d";

    private final RestTemplate restTemplate;
    private final String range;

    // ค่าตั้งค่ารับผ่าน constructor เท่านั้น (ไม่ฉีดเข้า field)
    public YahooFinancePriceSource(RestTemplate restTemplate,
                                   @Value("${market-data.yahoo.range:2y}") String range) {
        this.restTemplate = restTemplate;
        this.range = range;
    }

    @Override
    public String name() {
        return "Yahoo Finance";
    }

    // ระบบรองรับเฉพาะตลาดสหรัฐฯ (ดู UsMarket): AAPL -> AAPL, BRK.B -> BRK-B (Yahoo ใช้ขีดแทนจุด)
    // พันธบัตร/เงินสด/คริปโต -> ไม่ดึงราคา (ใช้ข้อมูลจำลองแทน)
    @Override
    public Optional<String> resolveTicker(Asset asset) {
        AssetType type = asset.getAssetType();
        if (type != AssetType.STOCK && type != AssetType.ETF) {
            return Optional.empty();
        }
        return Optional.of(asset.getSymbol().toUpperCase(Locale.ROOT).replace('.', '-'));
    }

    // SPX -> ^GSPC (S&P 500), DJI -> ^DJI (Dow Jones)
    @Override
    public Optional<String> resolveIndexTicker(String indexCode) {
        return UsMarket.indexTicker(indexCode);
    }

    @Override
    public List<PriceBar> fetchDailyHistory(String ticker) {
        try {
            URI uri = URI.create(String.format(CHART_URL,
                    URLEncoder.encode(ticker, StandardCharsets.UTF_8), range));
            HttpHeaders headers = new HttpHeaders();
            // Yahoo ปฏิเสธ request ที่ไม่มี User-Agent แบบเบราว์เซอร์
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.set(HttpHeaders.ACCEPT, "application/json");

            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<PriceBar> bars = parseChart(response.getBody());
            if (bars.isEmpty()) {
                log.warn("Yahoo ไม่มีข้อมูลราคาของ {}", ticker);
            }
            return bars;
        } catch (Exception e) {
            // 404 = ไม่มีหุ้นนี้, 429 = ถูกจำกัดการเรียก, timeout = เน็ตมีปัญหา -> ทั้งหมดให้ระบบใช้ทางสำรอง
            log.warn("ดึงราคาจาก Yahoo ไม่สำเร็จ ({}): {}", ticker, e.getMessage());
            return List.of();
        }
    }

    // โครงสร้าง response: chart.result[0].{meta.gmtoffset, timestamp[], indicators.quote[0].{open,high,low,close,volume}[]}
    // แยกเป็น static method เพื่อให้เขียน unit test ด้วย JSON ตัวอย่างได้โดยไม่ต้องต่ออินเทอร์เน็ต
    @SuppressWarnings("unchecked")
    static List<PriceBar> parseChart(Map<String, Object> body) {
        if (body == null || !(body.get("chart") instanceof Map<?, ?> chart)) {
            return List.of();
        }
        if (!(chart.get("result") instanceof List<?> results) || results.isEmpty()) {
            return List.of();
        }
        Map<String, Object> result = (Map<String, Object>) results.get(0);
        Map<String, Object> meta = (Map<String, Object>) result.getOrDefault("meta", Map.of());
        long gmtOffset = meta.get("gmtoffset") instanceof Number n ? n.longValue() : 0L;

        List<Object> timestamps = (List<Object>) result.get("timestamp");
        Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
        if (timestamps == null || indicators == null
                || !(indicators.get("quote") instanceof List<?> quotes) || quotes.isEmpty()) {
            return List.of();
        }
        Map<String, Object> quote = (Map<String, Object>) quotes.get(0);
        List<Object> opens = (List<Object>) quote.get("open");
        List<Object> highs = (List<Object>) quote.get("high");
        List<Object> lows = (List<Object>) quote.get("low");
        List<Object> closes = (List<Object>) quote.get("close");
        List<Object> volumes = (List<Object>) quote.get("volume");

        // TreeMap: เรียงตามวันที่ และถ้าวันเดียวกันซ้ำ (แท่งของวันนี้ที่ยังไม่ปิดตลาด) ใช้ตัวหลังสุด
        TreeMap<LocalDate, PriceBar> byDate = new TreeMap<>();
        for (int i = 0; i < timestamps.size(); i++) {
            BigDecimal close = toPrice(valueAt(closes, i));
            if (close == null || !(timestamps.get(i) instanceof Number ts)) {
                continue; // วันหยุด/ข้อมูลขาด Yahoo ส่ง null มา
            }
            BigDecimal open = orElse(toPrice(valueAt(opens, i)), close);
            BigDecimal high = orElse(toPrice(valueAt(highs, i)), close).max(open).max(close);
            BigDecimal low = orElse(toPrice(valueAt(lows, i)), close).min(open).min(close);
            Long volume = valueAt(volumes, i) instanceof Number v ? v.longValue() : 0L;
            LocalDate date = Instant.ofEpochSecond(ts.longValue() + gmtOffset).atOffset(ZoneOffset.UTC).toLocalDate();
            byDate.put(date, new PriceBar(date, open, high, low, close, volume));
        }
        return new ArrayList<>(byDate.values());
    }

    private static Object valueAt(List<Object> list, int i) {
        return list != null && i < list.size() ? list.get(i) : null;
    }

    private static BigDecimal toPrice(Object value) {
        if (!(value instanceof Number n)) {
            return null;
        }
        double d = n.doubleValue();
        if (Double.isNaN(d) || d <= 0) {
            return null;
        }
        return BigDecimal.valueOf(d).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal orElse(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }
}
