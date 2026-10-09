package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.dto.response.HoldingResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class HoldingMapper {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private HoldingMapper() {
    }

    // latestPrice = null (ยังไม่มีข้อมูลราคา) -> ใช้ต้นทุนเฉลี่ยเป็นราคาแทน กำไรจึงเป็น 0
    public static HoldingResponse toResponse(Holding holding, BigDecimal latestPrice) {
        BigDecimal price = latestPrice != null ? latestPrice : holding.getAvgCost();
        BigDecimal marketValue = holding.getQuantity().multiply(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal costValue = holding.getQuantity().multiply(holding.getAvgCost()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gain = marketValue.subtract(costValue);
        BigDecimal gainPercent = costValue.signum() > 0
                ? gain.multiply(HUNDRED).divide(costValue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new HoldingResponse(
                holding.getAsset().getId(), holding.getAsset().getSymbol(), holding.getAsset().getName(),
                holding.getQuantity(), holding.getAvgCost(), price,
                marketValue, costValue, gain, gainPercent, holding.getUpdatedAt(),
                holding.getAsset().getPriceSource());
    }
}
