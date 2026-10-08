package com.example.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PortfolioRequest(
        @NotBlank(message = "ชื่อพอร์ตห้ามว่าง") String name,
        @NotBlank(message = "สกุลเงินหลักห้ามว่าง") String baseCurrency
) {
}
