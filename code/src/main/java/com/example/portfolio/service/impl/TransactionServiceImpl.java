package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.TransactionRepository;
import com.example.portfolio.service.HoldingService;
import com.example.portfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// จุดเดียวที่ "บันทึก Transaction + อัปเดต Holding" ไปพร้อมกันใน transaction เดียว
// ทั้ง TransactionController และ BuyCommand/SellCommand (rebalance) เรียกผ่านที่นี่
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final HoldingService holdingService;

    @Override
    public List<Transaction> getByPortfolio(Long portfolioId) {
        return transactionRepository.findByPortfolioId(portfolioId, Pageable.unpaged()).getContent();
    }

    @Override
    @Transactional
    public Transaction record(Long portfolioId, Long assetId, TransactionType type,
                              BigDecimal quantity, BigDecimal price, LocalDateTime executedAt) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        // อัปเดต holding ก่อน: ถ้าขายเกินจำนวนที่ถือ จะ throw ออกไปและ rollback ทั้งหมด ไม่มี transaction ค้าง
        if (type.affectsHolding()) { // ประเภทรู้เองว่ากระทบ holding หรือไม่ (ไม่ต้องไล่เช็คทีละประเภท)
            holdingService.applyTransaction(portfolioId, assetId, type, quantity, price);
        }

        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(type)
                .quantity(quantity)
                .price(price)
                .executedAt(executedAt != null ? executedAt : LocalDateTime.now())
                .build();
        return transactionRepository.save(transaction);
    }
}
