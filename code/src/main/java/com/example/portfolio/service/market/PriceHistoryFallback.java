package com.example.portfolio.service.market;

import com.example.portfolio.domain.entity.Asset;

import java.math.BigDecimal;

// ทางสำรองเมื่อดึงราคาจริงไม่ได้ — PriceHistoryService ขึ้นกับ interface นี้ ไม่ใช่ตัวสร้างข้อมูลจำลองตรง ๆ (DIP)
public interface PriceHistoryFallback {

    // สร้างราคาย้อนหลังให้สินทรัพย์ที่ยังไม่มีราคาเลย (มีแล้วไม่ทำอะไร)
    void backfillIfMissing(Asset asset, BigDecimal anchorPrice);
}
