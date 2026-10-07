package com.example.portfolio.service.rebalance;

import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.service.TransactionService;

import java.math.BigDecimal;

public class SellCommand implements RebalanceCommand {

    private final TransactionService transactionService;
    private final Long portfolioId;
    private final Long assetId;
    private final BigDecimal quantity;
    private final BigDecimal price;

    public SellCommand(TransactionService transactionService, Long portfolioId, Long assetId,
                       BigDecimal quantity, BigDecimal price) {
        this.transactionService = transactionService;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public void execute() {
        transactionService.record(portfolioId, assetId, TransactionType.SELL, quantity, price, null);
    }
}
