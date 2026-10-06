package com.example.portfolio.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// ทดสอบว่าแต่ละประเภทธุรกรรมรู้ผลต่อจำนวนหน่วยที่ถือของตัวเอง (ใช้แทน switch ตามประเภท)
class TransactionTypeTest {

    @Test
    @DisplayName("BUY เพิ่มหน่วย, SELL ลดหน่วย, ประเภทอื่นไม่กระทบ")
    void signedQuantity() {
        BigDecimal four = new BigDecimal("4");
        assertThat(TransactionType.BUY.signedQuantity(four)).isEqualByComparingTo("4");
        assertThat(TransactionType.SELL.signedQuantity(four)).isEqualByComparingTo("-4");
        assertThat(TransactionType.DIVIDEND.signedQuantity(four)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("มีแค่ BUY และ SELL ที่กระทบ holding")
    void affectsHolding() {
        assertThat(TransactionType.values())
                .filteredOn(TransactionType::affectsHolding)
                .containsExactlyInAnyOrder(TransactionType.BUY, TransactionType.SELL);
    }

    @Test
    @DisplayName("ทิศของ SELL น้อยกว่า BUY (ใช้เรียงให้ขายก่อนซื้อตอนรีบาลานซ์)")
    void sellBeforeBuy() {
        assertThat(TransactionType.SELL.holdingDirection()).isLessThan(TransactionType.BUY.holdingDirection());
    }
}
