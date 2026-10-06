package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;

// หุ้นหนึ่งตัวในรายชื่ออ้างอิง (ยังไม่จำเป็นต้องอยู่ในตาราง assets)
public record SymbolInfo(String symbol, String name, AssetType assetType, String exchange) {
}
