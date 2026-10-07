package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;

public record TradeOrder(
        Long assetId,
        String symbol,
        TransactionType type, // BUY หรือ SELL
        BigDecimal quantity,
        BigDecimal estimatedPrice
) {
}
