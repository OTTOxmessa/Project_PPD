package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    List<Transaction> getByPortfolio(Long portfolioId);

    // executedAt = null หมายถึงเวลาปัจจุบัน
    Transaction record(Long portfolioId, Long assetId, TransactionType type,
                       BigDecimal quantity, BigDecimal price, LocalDateTime executedAt);
}
