package com.example.portfolio.mapper;

import com.example.portfolio.dto.response.TradeOrderResponse;
import com.example.portfolio.service.rebalance.TradeOrder;

public class TradeOrderMapper {

    private TradeOrderMapper() {
    }

    public static TradeOrderResponse toResponse(TradeOrder order) {
        return new TradeOrderResponse(
                order.assetId(), order.symbol(), order.type(), order.quantity(), order.estimatedPrice());
    }
}
