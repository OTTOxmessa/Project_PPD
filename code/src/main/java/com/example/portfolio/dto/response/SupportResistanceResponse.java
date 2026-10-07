package com.example.portfolio.dto.response;

import java.math.BigDecimal;

// pivot = จุดกึ่งกลาง (Pivot Point หรือค่าเฉลี่ย แล้วแต่วิธีที่เลือก)
public record SupportResistanceResponse(BigDecimal support, BigDecimal resistance, BigDecimal pivot) {
}
