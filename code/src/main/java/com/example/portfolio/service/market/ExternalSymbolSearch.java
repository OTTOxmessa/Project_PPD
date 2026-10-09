package com.example.portfolio.service.market;

import java.util.List;

// แหล่งค้นหาหุ้นภายนอก ใช้เมื่อหาในรายชื่อของระบบ (symbols.txt + ตาราง assets) ไม่พอ
// ผลลัพธ์ต้องผ่านกฎของ UsMarket แล้ว (หุ้น/ETF ตลาดสหรัฐฯ เท่านั้น) ถ้าค้นไม่ได้ให้คืน list ว่าง ห้ามโยน exception
public interface ExternalSymbolSearch {

    List<SymbolInfo> search(String query, int limit);
}
