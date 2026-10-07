package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull Long assetId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        @PastOrPresent(message = "วันที่ทำรายการต้องไม่เป็นวันในอนาคต") LocalDate executedAt // ไม่บังคับ
) {
}
