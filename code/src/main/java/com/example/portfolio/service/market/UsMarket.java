package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// ขอบเขตตลาดที่ระบบรองรับ: หุ้นและ ETF ในตลาดสหรัฐฯ ซื้อขายเป็น USD เท่านั้น
// รวมกฎไว้ที่เดียว ทั้งการสร้าง/แก้สินทรัพย์ การสร้างพอร์ต และการดึงราคาดัชนี
// ตลาดอื่น (เช่น SET) คริปโต และการแปลงสกุลเงิน อยู่ในแนวทางการพัฒนาต่อ (ดู README)
public final class UsMarket {

    public static final String CURRENCY = "USD";
    public static final String EXCHANGE = "US";

    // ticker สหรัฐฯ: ตัวอักษร 1-5 ตัว อาจมี class หุ้นต่อท้าย เช่น BRK.B, BF.B
    private static final Pattern TICKER = Pattern.compile("^[A-Z]{1,5}(\\.[A-Z])?$");

    private static final Set<AssetType> SUPPORTED_TYPES = EnumSet.of(AssetType.STOCK, AssetType.ETF);

    // ชื่อตลาดที่ผู้ใช้อาจกรอก ทั้งหมดถือเป็นตลาดสหรัฐฯ และเก็บเป็น "US"
    private static final Set<String> US_EXCHANGES = Set.of("US", "NYSE", "NASDAQ", "NYSEARCA", "NYSE ARCA", "AMEX", "CBOE");

    // รหัสดัชนีในระบบ -> ticker ของ Yahoo Finance
    private static final Map<String, String> INDEX_TICKERS = Map.of(
            "SPX", "^GSPC",  // S&P 500
            "DJI", "^DJI");  // Dow Jones Industrial Average

    private UsMarket() {
    }

    // ตรวจว่าเป็นสินทรัพย์ที่ระบบรองรับ แล้วคืนชื่อตลาดที่จัดรูปแล้ว ("US")
    // ไม่ผ่าน -> IllegalArgumentException (GlobalExceptionHandler ตอบ 400 พร้อมเหตุผล)
    public static String requireSupported(String symbol, AssetType type, String exchange) {
        String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        if (!TICKER.matcher(normalizedSymbol).matches()) {
            throw new IllegalArgumentException("symbol " + normalizedSymbol
                    + " ไม่ใช่ ticker ของตลาดสหรัฐฯ — ระบบรองรับเฉพาะหุ้นและ ETF ในตลาดสหรัฐฯ (USD)");
        }
        if (type != null && !SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("ระบบรองรับเฉพาะหุ้น (STOCK) และ ETF ในตลาดสหรัฐฯ ตอนนี้ — "
                    + type + " อยู่ในแนวทางการพัฒนาต่อ");
        }
        if (exchange != null && !exchange.isBlank()
                && !US_EXCHANGES.contains(exchange.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("ตลาด " + exchange.trim()
                    + " ยังไม่รองรับ — ระบบรองรับเฉพาะตลาดสหรัฐฯ (NYSE, NASDAQ) ที่ซื้อขายเป็น USD");
        }
        return EXCHANGE;
    }

    public static boolean isSupportedCurrency(String currency) {
        return currency != null && CURRENCY.equalsIgnoreCase(currency.trim());
    }

    public static Optional<String> indexTicker(String indexCode) {
        return indexCode == null ? Optional.empty()
                : Optional.ofNullable(INDEX_TICKERS.get(indexCode.trim().toUpperCase(Locale.ROOT)));
    }
}
