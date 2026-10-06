package com.example.portfolio.service.analysis;

import java.math.BigDecimal;

// ผลลัพธ์การคำนวณแนวรับ-แนวต้าน — pivot อาจเป็น mean แทน pivot point จริง ขึ้นกับ strategy ที่ใช้
public record SupportResistanceLevels(
        BigDecimal support,
        BigDecimal resistance,
        BigDecimal pivot
) {
}
