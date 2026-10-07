package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface HoldingService {

    // เฉพาะสินทรัพย์ที่ยังถืออยู่ (จำนวน > 0) — ตัวที่ขายหมดแล้วไม่ต้องแสดง
    List<Holding> getOpenPositions(Long portfolioId);

    // ปรับ holding ตามธุรกรรมที่เกิดขึ้น (BUY ถัวเฉลี่ยต้นทุน, SELL ลดจำนวน)
    Holding applyTransaction(Long portfolioId, Long assetId, TransactionType type,
                              BigDecimal quantity, BigDecimal price);
}
