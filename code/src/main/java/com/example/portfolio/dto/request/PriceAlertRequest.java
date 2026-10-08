package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.AlertCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PriceAlertRequest(
        @NotNull(message = "กรุณาเลือกสินทรัพย์") Long assetId,
        @NotNull(message = "กรุณาเลือกเงื่อนไขการแจ้งเตือน") AlertCondition condition,
        @NotNull(message = "กรุณากรอกราคาเป้าหมาย")
        @DecimalMin(value = "0", inclusive = false, message = "ราคาเป้าหมายต้องมากกว่า 0") BigDecimal targetPrice
) {
}
