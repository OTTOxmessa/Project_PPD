package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.service.TransactionService;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

// Factory ของ Command pattern: แปลง TradeOrder เป็น RebalanceCommand ตามประเภทคำสั่ง
// ใช้ตารางจับคู่ "ประเภท -> constructor" แทน switch — เพิ่มคำสั่งใหม่ = เพิ่มคลาส Command + ลงทะเบียนหนึ่งบรรทัด
public final class RebalanceCommands {

    @FunctionalInterface
    private interface CommandConstructor {
        RebalanceCommand create(TransactionService transactionService, Long portfolioId, Long assetId,
                                BigDecimal quantity, BigDecimal price);
    }

    private static final Map<TransactionType, CommandConstructor> REGISTRY = new EnumMap<>(TransactionType.class);

    static {
        REGISTRY.put(TransactionType.BUY, BuyCommand::new);
        REGISTRY.put(TransactionType.SELL, SellCommand::new);
    }

    private RebalanceCommands() {
    }

    public static RebalanceCommand forOrder(TradeOrder order, Long portfolioId, TransactionService transactionService) {
        CommandConstructor constructor = REGISTRY.get(order.type());
        if (constructor == null) {
            throw new IllegalArgumentException("รีบาลานซ์ไม่รองรับคำสั่งประเภท " + order.type());
        }
        return constructor.create(transactionService, portfolioId, order.assetId(), order.quantity(), order.estimatedPrice());
    }
}
