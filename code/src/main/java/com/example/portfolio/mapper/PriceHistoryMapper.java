package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.dto.response.PricePointResponse;

public class PriceHistoryMapper {

    private PriceHistoryMapper() {
    }

    public static PricePointResponse toResponse(PriceHistory p) {
        return new PricePointResponse(p.getPriceDate(), p.getOpen(), p.getHigh(),
                p.getLow(), p.getClose(), p.getVolume());
    }
}
