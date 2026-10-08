package com.example.portfolio.service.market.adapter;

import com.example.portfolio.service.market.MarketDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

// Adapter pattern: แปลง response จาก API ราคาหุ้นภายนอก (รูปแบบ Alpha Vantage GLOBAL_QUOTE)
// ให้เข้ากับ MarketDataProvider ที่ระบบออกแบบไว้เอง — ค่าตั้งค่ารับผ่าน constructor
// ตอนนี้ระบบใช้ PriceHistoryMarketDataProvider (@Primary) เป็นหลัก คลาสนี้เป็นทางเลือกเมื่อมี API key จริง
@Component
public class ExternalMarketDataAdapter implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    private final String apiKey;

    public ExternalMarketDataAdapter(RestTemplate restTemplate,
                                     @Value("${market-data.api-base-url:https://www.alphavantage.co/query}") String apiBaseUrl,
                                     @Value("${market-data.api-key:demo}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal getLatestPrice(String symbol) {
        String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s", apiBaseUrl, symbol, apiKey);

        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new IllegalStateException("ไม่ได้รับข้อมูลราคาจาก external API สำหรับ " + symbol);
        }

        // โครง response: { "Global Quote": { "05. price": "123.45", ... } }
        Object quote = response.get("Global Quote");
        if (!(quote instanceof Map<?, ?> quoteMap) || quoteMap.get("05. price") == null) {
            throw new IllegalStateException("รูปแบบ response ไม่ตรงตามที่คาดไว้สำหรับ " + symbol);
        }
        return new BigDecimal(quoteMap.get("05. price").toString());
    }
}
