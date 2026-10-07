package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id, Long assetId, String symbol, TransactionType type,
        BigDecimal quantity, BigDecimal price, LocalDateTime executedAt
) {
}
