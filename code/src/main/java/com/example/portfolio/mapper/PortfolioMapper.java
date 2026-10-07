package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.dto.request.PortfolioRequest;
import com.example.portfolio.dto.response.PortfolioResponse;
import com.example.portfolio.dto.response.PortfolioSummaryResponse;
import com.example.portfolio.service.PortfolioSummary;

public class PortfolioMapper {

    private PortfolioMapper() {
    }

    // request -> entity ใหม่ (ยังไม่มี id/เจ้าของ — service เป็นคนผูกกับผู้ใช้)
    public static Portfolio toEntity(PortfolioRequest request) {
        return Portfolio.builder()
                .name(request.name())
                .baseCurrency(request.baseCurrency())
                .build();
    }

    public static PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getId(), portfolio.getName(), portfolio.getBaseCurrency(), portfolio.getCreatedAt());
    }

    public static PortfolioSummaryResponse toSummaryResponse(PortfolioSummary summary) {
        return new PortfolioSummaryResponse(
                summary.id(), summary.name(), summary.baseCurrency(), summary.marketValue(),
                summary.costBasis(), summary.gain(), summary.gainPercent(), summary.holdingsCount());
    }
}
