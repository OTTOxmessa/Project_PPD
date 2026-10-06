package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByPortfolioId(Long portfolioId);

    // ใช้เช็คก่อน insert/update ตอนมี transaction ใหม่เข้ามา (upsert logic ใน HoldingService)
    Optional<Holding> findByPortfolioIdAndAssetId(Long portfolioId, Long assetId);
}
