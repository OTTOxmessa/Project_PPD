package com.example.portfolio.service.market;

import java.math.BigDecimal;

// Adapter pattern จะเข้ามาที่นี่ — Service อื่น ๆ ควรขึ้นกับ interface นี้
// ไม่ผูกกับ API ภายนอกเจ้าใดเจ้าหนึ่งตรง ๆ (DIP)
public interface MarketDataProvider {

    BigDecimal getLatestPrice(String symbol);
}
