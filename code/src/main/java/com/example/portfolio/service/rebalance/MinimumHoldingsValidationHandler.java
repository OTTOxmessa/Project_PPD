package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.entity.Portfolio;

import java.util.List;

// ตัวอย่าง concrete handler ตัวแรกของ chain — เพิ่มเข้ามาเพื่อให้ Chain of Responsibility ทำงานได้จริง
// (RebalanceValidationHandler เดิมมีแค่ abstract class ยังไม่มี handler จริงให้ทดสอบ)
// ทีมสามารถต่อ handler อื่นเพิ่มได้ เช่น เช็คเวลาตลาดเปิด, เช็คเงินสดคงเหลือพอไหม โดยสร้าง subclass ใหม่แบบนี้
public class MinimumHoldingsValidationHandler extends RebalanceValidationHandler {

    @Override
    protected void doValidate(Portfolio portfolio, List<TradeOrder> proposedTrades) {
        if (portfolio.getHoldings().isEmpty()) {
            throw new IllegalStateException("พอร์ตนี้ยังไม่มี holding ใด ๆ ให้ rebalance ได้");
        }
    }
}
