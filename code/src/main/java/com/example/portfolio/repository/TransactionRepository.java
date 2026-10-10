package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByPortfolioId(Long portfolioId, Pageable pageable);

    // แก้/ลบได้เฉพาะรายการของพอร์ตนี้ (กันการส่ง id ของพอร์ตอื่นมา)
    Optional<Transaction> findByIdAndPortfolioId(Long id, Long portfolioId);

    // ประวัติของหุ้นหนึ่งตัวในพอร์ต เรียงตามวันที่ทำรายการ (วันเดียวกันเรียงตามลำดับที่บันทึก) ใช้คำนวณ holding ใหม่
    List<Transaction> findByPortfolioIdAndAssetIdOrderByExecutedAtAscIdAsc(Long portfolioId, Long assetId);

    // ใช้สร้างกราฟมูลค่าพอร์ตย้อนหลังสำหรับฟีเจอร์ 4 (growth vs market)
    List<Transaction> findByPortfolioIdAndExecutedAtBetween(
            Long portfolioId, LocalDateTime start, LocalDateTime end);
}
