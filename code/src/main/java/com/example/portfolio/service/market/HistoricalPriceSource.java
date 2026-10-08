package com.example.portfolio.service.market;

import com.example.portfolio.domain.entity.Asset;

import java.util.List;
import java.util.Optional;

// Target interface ของ Adapter pattern: ระบบรู้จักแค่ interface นี้ ไม่ผูกกับผู้ให้บริการรายใด
// เปลี่ยนจาก Yahoo ไปเจ้าอื่นในอนาคต = เขียน implementation ใหม่ ไม่ต้องแก้ PriceHistoryService (OCP + DIP)
public interface HistoricalPriceSource {

    String name();

    // แปลงสินทรัพย์ของระบบเป็นรหัสของผู้ให้บริการ — empty = ผู้ให้บริการไม่มีข้อมูลสินทรัพย์นี้
    Optional<String> resolveTicker(Asset asset);

    Optional<String> resolveIndexTicker(String indexCode);

    // ราคารายวันย้อนหลัง เรียงจากเก่าไปใหม่ — คืน list ว่างเมื่อดึงไม่สำเร็จ (ไม่ throw)
    List<PriceBar> fetchDailyHistory(String ticker);
}
