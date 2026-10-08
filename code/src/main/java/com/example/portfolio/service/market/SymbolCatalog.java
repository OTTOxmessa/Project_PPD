package com.example.portfolio.service.market;

import java.util.List;
import java.util.Optional;

// แหล่งรายชื่อหุ้นอ้างอิง — วันนี้อ่านจากไฟล์ (SymbolDirectory) วันหน้าเปลี่ยนเป็น API ได้โดยไม่แก้ผู้ใช้ (DIP)
public interface SymbolCatalog {

    Optional<SymbolInfo> findBySymbol(String symbol);

    List<SymbolInfo> search(String query, int limit);
}
