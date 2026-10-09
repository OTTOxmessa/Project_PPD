package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ทดสอบขอบเขตตลาดที่ระบบรองรับ: หุ้นและ ETF ตลาดสหรัฐฯ (USD) เท่านั้น
class UsMarketTest {

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "brk.b", "BF.B", "T", "GOOGL"})
    @DisplayName("ticker สหรัฐฯ (รวม class หุ้น เช่น BRK.B) ผ่าน และตลาดว่างถือเป็น US")
    void acceptsUsTickers(String symbol) {
        assertThat(UsMarket.requireSupported(symbol, AssetType.STOCK, null)).isEqualTo("US");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NYSE", "nasdaq", "NYSE Arca", "US"})
    @DisplayName("ชื่อตลาดสหรัฐฯ ทุกแบบเก็บเป็น US")
    void normalizesUsExchanges(String exchange) {
        assertThat(UsMarket.requireSupported("VOO", AssetType.ETF, exchange)).isEqualTo("US");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PTT.BK", "BTC-USD", "TOOLONG", "123", ""})
    @DisplayName("symbol ที่ไม่ใช่รูปแบบ ticker สหรัฐฯ → IllegalArgumentException")
    void rejectsNonUsTickers(String symbol) {
        assertThatThrownBy(() -> UsMarket.requireSupported(symbol, AssetType.STOCK, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ตลาดสหรัฐฯ");
    }

    @Test
    @DisplayName("ตลาดอื่น (SET) → IllegalArgumentException พร้อมชื่อตลาด")
    void rejectsOtherExchanges() {
        assertThatThrownBy(() -> UsMarket.requireSupported("PTT", AssetType.STOCK, "SET"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SET");
    }

    @Test
    @DisplayName("คริปโตและพันธบัตรรายตัว ยังไม่รองรับ → IllegalArgumentException")
    void rejectsUnsupportedTypes() {
        assertThatThrownBy(() -> UsMarket.requireSupported("BTC", AssetType.CRYPTO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CRYPTO");
        assertThatThrownBy(() -> UsMarket.requireSupported("TBOND", AssetType.BOND, "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("สกุลเงินที่รองรับมีแค่ USD")
    void currency() {
        assertThat(UsMarket.isSupportedCurrency("usd")).isTrue();
        assertThat(UsMarket.isSupportedCurrency("THB")).isFalse();
        assertThat(UsMarket.isSupportedCurrency(null)).isFalse();
    }

    @Test
    @DisplayName("ดัชนี: SPX → ^GSPC, DJI → ^DJI, SET ไม่รองรับ")
    void indexTickers() {
        assertThat(UsMarket.indexTicker("SPX")).contains("^GSPC");
        assertThat(UsMarket.indexTicker("dji")).contains("^DJI");
        assertThat(UsMarket.indexTicker("SET")).isEmpty();
        assertThat(UsMarket.indexTicker(null)).isEmpty();
    }
}
