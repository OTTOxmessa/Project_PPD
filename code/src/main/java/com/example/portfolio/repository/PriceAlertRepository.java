package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByPortfolioId(Long portfolioId);

    // เมธอดหลักที่ PriceAlertScheduler เรียกทุกรอบ — กรองเฉพาะ alert ที่ยังรอตรวจสอบ
    List<PriceAlert> findByStatus(AlertStatus status);

    List<PriceAlert> findByStatusAndAssetId(AlertStatus status, Long assetId);
}
