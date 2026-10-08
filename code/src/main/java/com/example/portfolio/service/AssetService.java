package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.enums.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// จัดการข้อมูลสินทรัพย์ — การค้นหาชื่อหุ้น (auto-complete) แยกไปอยู่ที่ SymbolSearchService (ISP)
public interface AssetService {

    List<Asset> getAll();

    Asset getById(Long id);

    Asset create(Asset asset);

    // หา asset จาก symbol ถ้าไม่มีให้สร้างใหม่ (ข้อมูลจากรายชื่ออ้างอิง) — ใช้เพิ่มหุ้นจากหน้าพอร์ต/watchlist ได้ทันที
    Asset ensure(String symbol, String name, AssetType assetType, String exchange, BigDecimal referencePrice);

    List<PriceHistory> getPriceHistory(Long assetId, LocalDate from, LocalDate to);
}
