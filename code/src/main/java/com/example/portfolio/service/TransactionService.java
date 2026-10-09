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

    // แก้รายการเดิม (สินทรัพย์เปลี่ยนไม่ได้) แล้วคำนวณ holding ของสินทรัพย์นั้นใหม่จากประวัติทั้งหมด
    // executedAt = null หรือวันเดียวกับเดิม -> คงเวลาเดิมไว้
    Transaction update(Long portfolioId, Long transactionId, TransactionType type,
                       BigDecimal quantity, BigDecimal price, LocalDateTime executedAt);

    void delete(Long portfolioId, Long transactionId);
}
