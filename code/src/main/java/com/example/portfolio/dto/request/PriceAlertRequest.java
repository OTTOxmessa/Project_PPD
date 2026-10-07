package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.AlertCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PriceAlertRequest(
        @NotNull Long assetId,
        @NotNull AlertCondition condition,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal targetPrice
) {
}
