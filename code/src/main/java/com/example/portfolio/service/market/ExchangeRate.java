package com.example.portfolio.service.market;

import java.math.BigDecimal;
import java.time.LocalDate;

// อัตราแลกเปลี่ยน 1 base = rate quote เช่น 1 USD = 32.85 THB ณ วันที่ asOf (ราคาปิดล่าสุดจาก Yahoo)
public record ExchangeRate(String base, String quote, BigDecimal rate, LocalDate asOf) {
}
