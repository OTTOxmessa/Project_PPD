package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.MarketIndex;

import java.math.BigDecimal;

// ราคาย้อนหลังของสินทรัพย์หนึ่งตัว: ดึงราคาจริงจากแหล่งภายนอก ถ้าไม่ได้ใช้ข้อมูลจำลองแทน
public interface PriceHistoryService {

    // เรียกตอนเพิ่มสินทรัพย์ใหม่: ถ้ายังไม่มีราคาเลย ลองราคาจริงก่อน ไม่ได้ค่อยสร้างข้อมูลจำลอง
    void ensureHistory(Asset asset, BigDecimal anchorPrice);

    // ดึงราคาจริงมาแทนของเดิม — คืน false ถ้าดึงไม่ได้ (ข้อมูลเดิมยังอยู่ครบ)
    boolean importRealHistory(Asset asset);

    boolean importRealIndexHistory(MarketIndex index);
}
