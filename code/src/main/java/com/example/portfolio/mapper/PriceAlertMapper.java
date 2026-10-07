package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.dto.request.PriceAlertRequest;
import com.example.portfolio.dto.response.PriceAlertResponse;

public class PriceAlertMapper {

    private PriceAlertMapper() {
    }

    // พอร์ต/สินทรัพย์/สถานะ ให้ service เป็นคนกำหนด (ต้องตรวจว่ามีอยู่จริงก่อน)
    public static PriceAlert toEntity(PriceAlertRequest request) {
        return PriceAlert.builder()
                .condition(request.condition())
                .targetPrice(request.targetPrice())
                .build();
    }

    public static PriceAlertResponse toResponse(PriceAlert alert) {
        return new PriceAlertResponse(
                alert.getId(), alert.getAsset().getId(), alert.getAsset().getSymbol(),
                alert.getCondition(), alert.getTargetPrice(), alert.getStatus(), alert.getCreatedAt());
    }
}
