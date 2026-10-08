package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.dto.response.AllocationTargetResponse;

public class AllocationTargetMapper {

    private AllocationTargetMapper() {
    }

    public static AllocationTargetResponse toResponse(AllocationTarget target) {
        return new AllocationTargetResponse(
                target.getAsset().getId(), target.getAsset().getSymbol(), target.getTargetPercent());
    }
}
