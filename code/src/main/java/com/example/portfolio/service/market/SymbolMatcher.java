package com.example.portfolio.service.market;

import java.util.Locale;

// กติกาจัดอันดับผลค้นหาหุ้น ใช้ร่วมกันทั้งรายชื่ออ้างอิงและหุ้นที่อยู่ในระบบแล้ว
public final class SymbolMatcher {

    public static final int NO_MATCH = 4;

    private SymbolMatcher() {
    }

    // ยิ่งเลขน้อยยิ่งตรง: symbol ตรงทั้งคำ > symbol ขึ้นต้นด้วยคำค้น > ชื่อขึ้นต้นด้วยคำค้น > ชื่อมีคำค้นอยู่
    public static int rank(String symbol, String name, String upperQuery) {
        if (symbol.equals(upperQuery)) return 0;
        if (symbol.startsWith(upperQuery)) return 1;
        String upperName = name == null ? "" : name.toUpperCase(Locale.ROOT);
        if (upperName.startsWith(upperQuery)) return 2;
        if (upperName.contains(upperQuery)) return 3;
        return NO_MATCH;
    }
}
