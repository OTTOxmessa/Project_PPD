package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.SupportResistanceResponse;
import com.example.portfolio.service.analysis.SupportResistanceLevels;

public class SupportResistanceMapper {

    private SupportResistanceMapper() {
    }

    public static SupportResistanceResponse toResponse(SupportResistanceLevels levels) {
        return new SupportResistanceResponse(levels.support(), levels.resistance(), levels.pivot());
    }
}
