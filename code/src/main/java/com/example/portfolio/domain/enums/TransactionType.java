package com.example.portfolio.domain.enums;

import java.math.BigDecimal;

// แต่ละประเภทรู้เองว่ากระทบจำนวนหน่วยที่ถืออย่างไร (+1 เพิ่ม, -1 ลด, 0 ไม่กระทบ)
// โค้ดที่ต้องรู้ผลต่อจำนวนหน่วยจึงถามจาก enum ได้เลย ไม่ต้องเขียน if/switch ตามประเภทซ้ำในหลายที่ (OCP)
public enum TransactionType {
    BUY(1),
    SELL(-1),
    DIVIDEND(0),
    DEPOSIT(0),
    WITHDRAWAL(0);

    private final int holdingDirection;

    TransactionType(int holdingDirection) {
        this.holdingDirection = holdingDirection;
    }

    public int holdingDirection() {
        return holdingDirection;
    }

    public boolean affectsHolding() {
        return holdingDirection != 0;
    }

    // จำนวนหน่วยแบบมีเครื่องหมาย เช่น SELL 4 หน่วย -> -4
    public BigDecimal signedQuantity(BigDecimal quantity) {
        return quantity.multiply(BigDecimal.valueOf(holdingDirection));
    }
}
