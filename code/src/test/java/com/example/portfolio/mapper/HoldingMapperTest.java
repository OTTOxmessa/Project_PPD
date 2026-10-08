package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.PriceSource;
import com.example.portfolio.dto.response.HoldingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// ทดสอบการแปลง Holding เป็น HoldingResponse: มูลค่า กำไร และแหล่งราคา (จริง/จำลอง)
class HoldingMapperTest {

    private static Holding holding(PriceSource source) {
        Asset asset = Asset.builder().id(10L).symbol("PTT").name("PTT Public Company").priceSource(source).build();
        return Holding.builder().asset(asset)
                .quantity(new BigDecimal("15"))
                .avgCost(new BigDecimal("110"))
                .build();
    }

    @Test
    @DisplayName("15 หุ้น ต้นทุน 110 ราคาล่าสุด 120 → มูลค่า 1,800 กำไร 150 (+9.09%)")
    void calculatesValueAndGain() {
        HoldingResponse response = HoldingMapper.toResponse(holding(PriceSource.YAHOO), new BigDecimal("120"));

        assertThat(response.marketValue()).isEqualByComparingTo("1800");
        assertThat(response.costValue()).isEqualByComparingTo("1650");
        assertThat(response.gain()).isEqualByComparingTo("150");
        assertThat(response.gainPercent()).isEqualByComparingTo("9.09");
    }

    @Test
    @DisplayName("ส่งแหล่งราคาของสินทรัพย์ต่อไปให้หน้าเว็บ เพื่อแสดงป้าย \"จำลอง\"")
    void passesPriceSource() {
        assertThat(HoldingMapper.toResponse(holding(PriceSource.SYNTHETIC), BigDecimal.ONE).priceSource())
                .isEqualTo(PriceSource.SYNTHETIC);
        assertThat(HoldingMapper.toResponse(holding(PriceSource.YAHOO), BigDecimal.ONE).priceSource())
                .isEqualTo(PriceSource.YAHOO);
    }

    @Test
    @DisplayName("ยังไม่มีราคาล่าสุด → ใช้ต้นทุนเฉลี่ยเป็นราคา กำไรเป็น 0")
    void noLatestPriceUsesAverageCost() {
        HoldingResponse response = HoldingMapper.toResponse(holding(PriceSource.SYNTHETIC), null);

        assertThat(response.latestPrice()).isEqualByComparingTo("110");
        assertThat(response.gain()).isEqualByComparingTo("0");
    }
}
