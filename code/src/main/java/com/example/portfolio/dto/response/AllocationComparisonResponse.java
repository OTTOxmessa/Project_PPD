package com.example.portfolio.dto.response;

import java.math.BigDecimal;

// driftPercent = currentPercent - targetPercent (บวก = เกินเป้า, ลบ = ต่ำกว่าเป้า)
public record AllocationComparisonResponse(
        Long assetId,
        String symbol,
        BigDecimal currentPercent,
        BigDecimal targetPercent,
        BigDecimal driftPercent
) {
}
