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

// จุดเดียวที่ "บันทึก/แก้/ลบ Transaction + อัปเดต Holding" ไปพร้อมกันใน transaction เดียว
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

    @Override
    @Transactional
    public Transaction update(Long portfolioId, Long transactionId, TransactionType type,
                              BigDecimal quantity, BigDecimal price, LocalDateTime executedAt) {
        Transaction transaction = findInPortfolio(portfolioId, transactionId);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setPrice(price);
        // เปลี่ยนวันที่เท่านั้นจึงเปลี่ยนเวลา — แก้แค่ราคา/จำนวน ลำดับในวันเดิมต้องไม่เลื่อน
        if (executedAt != null && !executedAt.toLocalDate().equals(transaction.getExecutedAt().toLocalDate())) {
            transaction.setExecutedAt(executedAt);
        }
        Transaction saved = transactionRepository.save(transaction);
        rebuildHolding(portfolioId, transaction.getAsset().getId());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long portfolioId, Long transactionId) {
        Transaction transaction = findInPortfolio(portfolioId, transactionId);
        Long assetId = transaction.getAsset().getId();
        transactionRepository.delete(transaction);
        rebuildHolding(portfolioId, assetId);
    }

    private Transaction findInPortfolio(Long portfolioId, Long transactionId) {
        return transactionRepository.findByIdAndPortfolioId(transactionId, portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }

    // ทั้ง update และ delete อยู่ใน @Transactional เดียวกัน: ถ้า holding คำนวณใหม่แล้วติดลบ ทุกอย่าง rollback
    // query นี้ทำให้ Hibernate flush การแก้/ลบข้างบนก่อน ประวัติที่ได้จึงเป็นค่าใหม่แล้ว
    private void rebuildHolding(Long portfolioId, Long assetId) {
        holdingService.recalculate(portfolioId, assetId,
                transactionRepository.findByPortfolioIdAndAssetIdOrderByExecutedAtAscIdAsc(portfolioId, assetId));
    }
}
