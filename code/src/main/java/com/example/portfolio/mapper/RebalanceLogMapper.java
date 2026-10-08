package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.RebalanceLog;
import com.example.portfolio.dto.response.RebalanceLogResponse;

public class RebalanceLogMapper {

    private RebalanceLogMapper() {
    }

    public static RebalanceLogResponse toResponse(RebalanceLog log) {
        return new RebalanceLogResponse(log.getId(), log.getMethod(), log.getTriggeredAt(), log.getDetails());
    }
}
