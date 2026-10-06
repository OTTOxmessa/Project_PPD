package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;

// ราคาล่าสุด + การเปลี่ยนแปลงเทียบวันก่อนหน้า (ค่าราคาเป็น null ถ้ายังไม่มีข้อมูลราคา)
// priceSource: "YAHOO" = ราคาจริง, "SYNTHETIC" หรือ null = ข้อมูลจำลอง
public record Quote(
        Long assetId,
        String symbol,
        String name,
        AssetType assetType,
        String exchange,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal change,
        BigDecimal changePercent,
        LocalDate asOf,
        String priceSource
) {
}
