package com.example.portfolio.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AllocationTargetRequest(
        @NotNull(message = "กรุณาเลือกสินทรัพย์") Long assetId,
        @NotNull(message = "กรุณากรอกสัดส่วนเป้าหมาย")
        @DecimalMin(value = "0", message = "สัดส่วนเป้าหมายต้องไม่ติดลบ")
        @DecimalMax(value = "100", message = "สัดส่วนเป้าหมายต้องไม่เกิน 100%") BigDecimal targetPercent
) {
}
