package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;

// ราคาล่าสุด + การเปลี่ยนแปลงเทียบวันก่อนหน้า (ราคาเป็น null ถ้ายังไม่มีข้อมูล)
// priceSource: "YAHOO" = ราคาจริง, "SYNTHETIC" หรือไม่มีค่า = ข้อมูลจำลอง
public record QuoteResponse(
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
