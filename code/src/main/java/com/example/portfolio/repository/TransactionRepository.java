package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByPortfolioId(Long portfolioId, Pageable pageable);

    // ใช้สร้างกราฟมูลค่าพอร์ตย้อนหลังสำหรับฟีเจอร์ 4 (growth vs market)
    List<Transaction> findByPortfolioIdAndExecutedAtBetween(
            Long portfolioId, LocalDateTime start, LocalDateTime end);
}
