package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.service.TransactionService;

import java.math.BigDecimal;

// Command pattern — สร้างด้วย new ใน RebalanceService (ไม่ใช่ Spring bean) เพราะพารามิเตอร์เปลี่ยนทุกครั้ง
// เรียกผ่าน TransactionService จึงได้ทั้งประวัติ Transaction และ Holding ที่อัปเดตแล้ว
public class BuyCommand implements RebalanceCommand {

    private final TransactionService transactionService;
    private final Long portfolioId;
    private final Long assetId;
    private final BigDecimal quantity;
    private final BigDecimal price;

    public BuyCommand(TransactionService transactionService, Long portfolioId, Long assetId,
                      BigDecimal quantity, BigDecimal price) {
        this.transactionService = transactionService;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public void execute() {
        transactionService.record(portfolioId, assetId, TransactionType.BUY, quantity, price, null);
    }
}
