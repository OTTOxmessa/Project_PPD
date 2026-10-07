package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.Portfolio;

import java.util.List;

// ห่วงที่ 2 ของ chain: ต้องตั้งสัดส่วนเป้าหมายก่อน
// ถ้าไม่มีเป้าเลย ทุกสินทรัพย์จะถูกมองว่าเป้า = 0% และแผนจะสั่งขายทั้งพอร์ต
public class AllocationTargetsDefinedHandler extends RebalanceValidationHandler {

    @Override
    protected void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
        if (portfolio.getAllocationTargets().isEmpty()) {
            throw new IllegalStateException("ยังไม่ได้ตั้งสัดส่วนเป้าหมายของพอร์ตนี้ — ตั้งในแท็บสัดส่วนก่อนรีบาลานซ์");
        }
    }
}
