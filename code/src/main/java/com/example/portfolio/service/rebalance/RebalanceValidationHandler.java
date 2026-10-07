package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.Portfolio;

import java.util.List;

// Chain of Responsibility: แต่ละ handler เช็คเงื่อนไขของตัวเอง แล้วส่งต่อให้ handler ถัดไป
// เพิ่มเงื่อนไขใหม่ = สร้าง subclass ใหม่ต่อ chain ไม่ต้องแก้ handler เดิม (OCP)
public abstract class RebalanceValidationHandler {

    private RebalanceValidationHandler next;

    public RebalanceValidationHandler setNext(RebalanceValidationHandler next) {
        this.next = next;
        return next;
    }

    public final void validate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
        doValidate(portfolio, proposedTrades);
        if (next != null) {
            next.validate(portfolio, proposedTrades);
        }
    }

    // throw exception (เช่น IllegalStateException) ถ้าเงื่อนไขไม่ผ่าน
    protected abstract void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades);
}
