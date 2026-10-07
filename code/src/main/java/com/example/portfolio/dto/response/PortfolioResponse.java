package com.example.portfolio.dto.response;

import java.time.LocalDateTime;

public record PortfolioResponse(Long id, String name, String baseCurrency, LocalDateTime createdAt) {
}
