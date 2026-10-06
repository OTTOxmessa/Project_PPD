package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.service.market.Quote;

import java.math.BigDecimal;
import java.util.Optional;

// ราคาล่าสุด และ % เปลี่ยนแปลงรายวัน — ใช้ร่วมกันใน Watchlist, กระดานเทรด, Holdings, สรุปพอร์ต
public interface QuoteService {

    Quote getQuote(Asset asset);

    Optional<BigDecimal> latestPrice(Long assetId);
}
