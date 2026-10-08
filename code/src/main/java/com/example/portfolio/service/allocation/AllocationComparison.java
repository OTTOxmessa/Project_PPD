package com.example.portfolio.service.allocation;

import java.math.BigDecimal;

// ผลลัพธ์เปรียบเทียบสัดส่วนปัจจุบัน vs เป้าหมาย ต่อ asset หนึ่งตัว
public record AllocationComparison(
        Long assetId,
        String symbol,
        BigDecimal currentPercent,
        BigDecimal targetPercent,
        BigDecimal driftPercent // currentPercent - targetPercent (บวก = เกินเป้า, ลบ = ต่ำกว่าเป้า)
) {
}
