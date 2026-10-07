package com.example.portfolio.service;

import com.example.portfolio.service.market.SymbolSuggestion;

import java.util.List;

// Auto-complete ช่องค้นหาหุ้น: รวมหุ้นที่อยู่ในระบบแล้วกับรายชื่ออ้างอิง จัดอันดับตามความตรง
public interface SymbolSearchService {

    List<SymbolSuggestion> search(String query, int limit);
}
