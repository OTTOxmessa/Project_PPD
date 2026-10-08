package com.example.portfolio.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

// แท่งราคารายวันที่ได้จากแหล่งภายนอก (ก่อนแปลงเป็น entity PriceHistory)
public record PriceBar(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low,
                       BigDecimal close, Long volume) {
}
