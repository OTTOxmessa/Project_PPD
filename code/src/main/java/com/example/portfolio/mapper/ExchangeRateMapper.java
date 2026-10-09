package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.ExchangeRateResponse;
import com.example.portfolio.service.market.ExchangeRate;

public class ExchangeRateMapper {

    private ExchangeRateMapper() {
    }

    public static ExchangeRateResponse toResponse(ExchangeRate rate) {
        return new ExchangeRateResponse(rate.base(), rate.quote(), rate.rate(), rate.asOf());
    }
}
