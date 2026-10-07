package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.AllocationComparisonResponse;
import com.example.portfolio.service.allocation.AllocationComparison;

public class AllocationComparisonMapper {

    private AllocationComparisonMapper() {
    }

    public static AllocationComparisonResponse toResponse(AllocationComparison comparison) {
        return new AllocationComparisonResponse(
                comparison.assetId(), comparison.symbol(), comparison.currentPercent(),
                comparison.targetPercent(), comparison.driftPercent());
    }
}
