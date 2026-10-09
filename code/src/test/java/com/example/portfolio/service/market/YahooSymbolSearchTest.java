package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบ Adapter ค้นหาหุ้นจาก Yahoo: รับเฉพาะหุ้น/ETF ตลาดสหรัฐฯ และไม่ทำให้ระบบพังเมื่อ Yahoo ใช้ไม่ได้
@ExtendWith(MockitoExtension.class)
class YahooSymbolSearchTest {

    @Mock
    private RestTemplate restTemplate;

    private static Map<String, Object> quote(String symbol, String longname, String shortname,
                                             String quoteType, String exchange) {
        Map<String, Object> q = new HashMap<>();
        q.put("symbol", symbol);
        q.put("longname", longname);
        q.put("shortname", shortname);
        q.put("quoteType", quoteType);
        q.put("exchange", exchange);
        return q;
    }

    private static Map<String, Object> body(Map<?, ?>... quotes) {
        return Map.of("quotes", List.of(quotes));
    }

    @Test
    @DisplayName("parse: เก็บหุ้น NASDAQ/NYSE และ ETF ของ NYSE Arca ใช้ longname เป็นชื่อ ตลาดเก็บเป็น US")
    void keepsUsStocksAndEtfs() {
        List<SymbolInfo> result = YahooSymbolSearch.parse(body(
                quote("RKLB", "Rocket Lab Corporation", "Rocket Lab", "EQUITY", "NCM"),
                quote("EOSE", "Eos Energy Enterprises, Inc.", "Eos Energy", "EQUITY", "NMS"),
                quote("ARKX", "ARK Space & Defense Innovation ETF", "ARK Space", "ETF", "BTS"),
                quote("SPY", null, "SPDR S&P 500", "ETF", "PCX")));

        assertThat(result).containsExactly(
                new SymbolInfo("RKLB", "Rocket Lab Corporation", AssetType.STOCK, "US"),
                new SymbolInfo("EOSE", "Eos Energy Enterprises, Inc.", AssetType.STOCK, "US"),
                new SymbolInfo("ARKX", "ARK Space & Defense Innovation ETF", AssetType.ETF, "US"),
                new SymbolInfo("SPY", "SPDR S&P 500", AssetType.ETF, "US"));
    }

    @Test
    @DisplayName("parse: ตัดหุ้นนอกตลาด (OTC/Pink), ตลาดต่างประเทศ, คริปโต, กองทุนรวม และดัชนีออก")
    void dropsUnsupported() {
        List<SymbolInfo> result = YahooSymbolSearch.parse(body(
                quote("RKLBF", "Some OTC Co", null, "EQUITY", "PNK"),
                quote("PTT.BK", "PTT PCL", null, "EQUITY", "SET"),
                quote("BTC-USD", "Bitcoin USD", null, "CRYPTOCURRENCY", "CCC"),
                quote("VFIAX", "Vanguard 500 Index Admiral", null, "MUTUALFUND", "NAS"),
                quote("^GSPC", "S&P 500", null, "INDEX", "SNP")));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parse: BRK-B ของ Yahoo → BRK.B แบบเดียวกับรายชื่อในระบบ และตัด warrant ที่ ticker ยาวเกินออก")
    void normalizesClassShares() {
        List<SymbolInfo> result = YahooSymbolSearch.parse(body(
                quote("BRK-B", "Berkshire Hathaway Inc.", null, "EQUITY", "NYQ"),
                quote("ABCDEW", "Some Corp Warrant", null, "EQUITY", "NCM")));

        assertThat(result).extracting(SymbolInfo::symbol).containsExactly("BRK.B");
    }

    @Test
    @DisplayName("parse: response ว่างหรือไม่มี quotes → list ว่าง")
    void emptyBody() {
        assertThat(YahooSymbolSearch.parse(null)).isEmpty();
        assertThat(YahooSymbolSearch.parse(Map.of("news", List.of()))).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("search: คำค้นเดิมเรียก Yahoo ครั้งเดียว ครั้งต่อไปใช้ผลที่จำไว้")
    void cachesResults() {
        ResponseEntity response = ResponseEntity.ok(body(quote("RKLB", "Rocket Lab Corporation", null, "EQUITY", "NCM")));
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
        YahooSymbolSearch search = new YahooSymbolSearch(restTemplate);

        assertThat(search.search("rklb", 10)).extracting(SymbolInfo::symbol).containsExactly("RKLB");
        assertThat(search.search("RKLB", 10)).extracting(SymbolInfo::symbol).containsExactly("RKLB");
        verify(restTemplate, times(1)).exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("search: Yahoo ใช้ไม่ได้ (timeout/429) → คืน list ว่าง ไม่โยน exception")
    void failureReturnsEmpty() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThat(new YahooSymbolSearch(restTemplate).search("RKLB", 10)).isEmpty();
    }
}
