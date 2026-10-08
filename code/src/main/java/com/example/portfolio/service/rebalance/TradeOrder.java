package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public record TradeOrder(
        Long assetId,
        String symbol,
        TransactionType type, // BUY หรือ SELL
        BigDecimal quantity,
        BigDecimal estimatedPrice
) {

    // สร้างคำสั่งจาก "มูลค่าที่ต้องปรับ" (บวก = ซื้อเพิ่ม, ลบ = ขายออก) ใช้ร่วมกันทุก RebalanceStrategy
    // หุ้น/ETF ปัดลงเป็นจำนวนเต็ม เพราะซื้อขายเศษหุ้นไม่ได้ ถ้าปัดแล้วเหลือ 0 หน่วยจะไม่สร้างคำสั่ง
    public static Optional<TradeOrder> forValueChange(Asset asset, BigDecimal diffValue, BigDecimal price) {
        BigDecimal quantity = diffValue.divide(price, 6, RoundingMode.HALF_UP).abs();
        if (asset.getAssetType() != null && asset.getAssetType().wholeUnitsOnly()) {
            quantity = quantity.setScale(0, RoundingMode.DOWN);
        }
        if (quantity.signum() == 0) {
            return Optional.empty();
        }
        TransactionType type = diffValue.signum() > 0 ? TransactionType.BUY : TransactionType.SELL;
        return Optional.of(new TradeOrder(asset.getId(), asset.getSymbol(), type, quantity, price));
    }
}
