package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceAlertResponse(
        Long id, Long assetId, String symbol, AlertCondition condition,
        BigDecimal targetPrice, AlertStatus status, LocalDateTime createdAt
) {
}
