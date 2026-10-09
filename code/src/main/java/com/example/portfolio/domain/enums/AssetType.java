package com.example.portfolio.domain.enums;

// wholeUnitsOnly = ซื้อขายได้เฉพาะจำนวนเต็ม (หุ้นและ ETF ในตลาดซื้อเศษหุ้นไม่ได้)
// ส่วนคริปโตและกองทุนรวมซื้อเป็นทศนิยมได้ — โค้ดที่ต้องรู้เรื่องนี้ถาม enum ได้เลย ไม่ต้องเขียน if ตามประเภทเอง
public enum AssetType {
    STOCK(true),
    ETF(true),
    BOND(false),
    MUTUAL_FUND(false),
    CRYPTO(false),
    CASH(false);

    private final boolean wholeUnitsOnly;

    AssetType(boolean wholeUnitsOnly) {
        this.wholeUnitsOnly = wholeUnitsOnly;
    }

    public boolean wholeUnitsOnly() {
        return wholeUnitsOnly;
    }
}
