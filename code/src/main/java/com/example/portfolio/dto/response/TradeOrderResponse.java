package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;

// คำสั่งซื้อ/ขายหนึ่งรายการในแผนรีบาลานซ์
public record TradeOrderResponse(
        Long assetId,
        String symbol,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal estimatedPrice
) {
}
