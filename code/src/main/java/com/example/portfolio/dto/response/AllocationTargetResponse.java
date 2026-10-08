package com.example.portfolio.dto.response;

import java.math.BigDecimal;

public record AllocationTargetResponse(Long assetId, String symbol, BigDecimal targetPercent) {
}
