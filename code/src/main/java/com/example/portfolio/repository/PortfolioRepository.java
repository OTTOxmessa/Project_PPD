package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUserId(Long userId);

    // รองรับ pagination + sorting ตาม endpoint GET /api/v1/portfolios ที่ออกแบบไว้
    Page<Portfolio> findByUserId(Long userId, Pageable pageable);

    // เช็คว่า portfolio นี้เป็นของ user ที่ request เข้ามาจริงหรือไม่ (กัน user อื่นเข้าถึงพอร์ตคนอื่น)
    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);
}
