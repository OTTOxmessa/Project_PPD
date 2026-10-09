package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Adapter: แปลงผลค้นหาของ Yahoo Finance (/v1/finance/search) เป็น SymbolInfo ของระบบ
// ทำให้ค้นเจอหุ้นทุกตัวที่ซื้อขายใน NYSE / NASDAQ / NYSE American / NYSE Arca / Cboe
// แม้ไม่อยู่ใน symbols.txt (เช่น RKLB, EOSE) — ตัดหุ้นนอกตลาด (OTC/Pink) ตลาดต่างประเทศ และคริปโตออก
@Component
public class YahooSymbolSearch implements ExternalSymbolSearch {

    private static final Logger log = LoggerFactory.getLogger(YahooSymbolSearch.class);
    private static final String SEARCH_URL =
            "https://query1.finance.yahoo.com/v1/finance/search?q=%s&quotesCount=%d&newsCount=0&listsCount=0";

    // รหัสตลาดของ Yahoo ที่เป็นตลาดหลักทรัพย์สหรัฐฯ
    // NMS/NGM/NCM = NASDAQ, NYQ = NYSE, ASE = NYSE American, PCX = NYSE Arca, BTS = Cboe BZX
    private static final Set<String> US_EXCHANGE_CODES =
            Set.of("NMS", "NGM", "NCM", "NAS", "NYQ", "NYS", "ASE", "PCX", "BTS");

    // quoteType ของ Yahoo -> ประเภทสินทรัพย์ของระบบ (ประเภทอื่น เช่น CRYPTOCURRENCY, MUTUALFUND, INDEX ไม่รับ)
    private static final Map<String, AssetType> TYPES = Map.of("EQUITY", AssetType.STOCK, "ETF", AssetType.ETF);

    // จำผลค้นหาไว้ ไม่ให้ยิง Yahoo ซ้ำทุกครั้งที่พิมพ์คำเดิม (ล้างทั้งหมดเมื่อเกิน 500 คำ)
    private static final int MAX_CACHE = 500;
    private final Map<String, List<SymbolInfo>> cache = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;

    public YahooSymbolSearch(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<SymbolInfo> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        if (q.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<SymbolInfo> cached = cache.get(q);
        if (cached != null) {
            return cached.stream().limit(limit).toList();
        }
        try {
            // ขอมาเผื่อ เพราะผลบางส่วนจะถูกกรองทิ้ง (ตลาดต่างประเทศ, OTC)
            URI uri = URI.create(String.format(SEARCH_URL, URLEncoder.encode(q, StandardCharsets.UTF_8), 20));
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.set(HttpHeaders.ACCEPT, "application/json");
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<SymbolInfo> found = parse(response.getBody());
            if (cache.size() >= MAX_CACHE) {
                cache.clear();
            }
            cache.put(q, found);
            return found.stream().limit(limit).toList();
        } catch (Exception e) {
            // ค้นจาก Yahoo ไม่ได้ (429, timeout, เน็ตหลุด) -> ใช้แค่ผลจากรายชื่อในระบบ ไม่ทำให้หน้าเว็บพัง
            log.warn("ค้นหาหุ้นจาก Yahoo ไม่สำเร็จ ({}): {}", q, e.getMessage());
            return List.of();
        }
    }

    // โครงสร้าง response: quotes[].{symbol, shortname, longname, quoteType, exchange}
    // แยกเป็น static method เพื่อให้เขียน unit test ด้วย JSON ตัวอย่างได้โดยไม่ต้องต่ออินเทอร์เน็ต
    static List<SymbolInfo> parse(Map<?, ?> body) {
        if (body == null || !(body.get("quotes") instanceof List<?> quotes)) {
            return List.of();
        }
        List<SymbolInfo> result = new ArrayList<>();
        for (Object item : quotes) {
            if (item instanceof Map<?, ?> quote) {
                toSymbolInfo(quote).ifPresent(result::add);
            }
        }
        return result;
    }

    private static Optional<SymbolInfo> toSymbolInfo(Map<?, ?> quote) {
        AssetType type = TYPES.get(code(quote.get("quoteType")));
        if (type == null || !US_EXCHANGE_CODES.contains(code(quote.get("exchange")))) {
            return Optional.empty();
        }
        // Yahoo ใช้ขีดแทนจุดใน class หุ้น (BRK-B) ระบบเก็บเป็นจุด (BRK.B) แบบเดียวกับ symbols.txt
        String symbol = code(quote.get("symbol")).replace('-', '.');
        try {
            UsMarket.requireSupported(symbol, type, UsMarket.EXCHANGE);
        } catch (IllegalArgumentException notUsTicker) {
            return Optional.empty(); // เช่น warrant/unit ที่ ticker ยาวเกิน 5 ตัว
        }
        String name = text(quote.get("longname"));
        if (name.isEmpty()) {
            name = text(quote.get("shortname"));
        }
        return Optional.of(new SymbolInfo(symbol, name.isEmpty() ? symbol : name, type, UsMarket.EXCHANGE));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String code(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }
}
