package com.example.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PortfolioRequest(
        @NotBlank(message = "ชื่อพอร์ตห้ามว่าง") String name,
        @NotBlank(message = "สกุลเงินหลักห้ามว่าง")
        @Pattern(regexp = "USD", message = "ระบบรองรับเฉพาะพอร์ตสกุลเงิน USD (หุ้นและ ETF ตลาดสหรัฐฯ)") String baseCurrency
) {
}
