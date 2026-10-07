package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.AssetType;

// ผลลัพธ์ของช่องค้นหาหุ้น — assetId เป็น null ถ้ายังไม่เคยเพิ่มหุ้นนี้เข้าระบบ
public record SymbolSuggestionResponse(String symbol, String name, AssetType assetType, String exchange, Long assetId) {
}
