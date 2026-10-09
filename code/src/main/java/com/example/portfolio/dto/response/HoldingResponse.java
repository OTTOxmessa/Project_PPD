package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.PriceSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoldingResponse(
        Long assetId,
        String symbol,
        String name,
        BigDecimal quantity,
        BigDecimal avgCost,
        BigDecimal latestPrice,
        BigDecimal marketValue,
        BigDecimal costValue,
        BigDecimal gain,
        BigDecimal gainPercent,
        LocalDateTime updatedAt,
        PriceSource priceSource // YAHOO = ราคาจริง, SYNTHETIC = ราคาจำลอง (หน้าเว็บแสดงป้าย "จำลอง")
) {
}
