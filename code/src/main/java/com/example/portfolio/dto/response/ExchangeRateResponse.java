package com.example.portfolio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

// 1 base = rate quote เช่น {"base":"USD","quote":"THB","rate":32.85,"asOf":"2026-10-09"}
public record ExchangeRateResponse(String base, String quote, BigDecimal rate, LocalDate asOf) {
}
