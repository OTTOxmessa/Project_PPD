package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceAlertRepository;
import com.example.portfolio.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// จัดการ alert ที่ผู้ใช้ตั้งไว้ (CRUD) — การตรวจราคาอยู่ที่ PriceAlertMonitorServiceImpl (SRP)
@Service
@RequiredArgsConstructor
public class PriceAlertServiceImpl implements PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public PriceAlert create(Long portfolioId, Long assetId, PriceAlert alert) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
        alert.setPortfolio(portfolio);
        alert.setAsset(findAsset(assetId));
        alert.setStatus(AlertStatus.PENDING);
        return priceAlertRepository.save(alert);
    }

    @Override
    public List<PriceAlert> getByPortfolio(Long portfolioId) {
        return priceAlertRepository.findByPortfolioId(portfolioId);
    }

    @Override
    public PriceAlert getById(Long portfolioId, Long alertId) {
        return findInPortfolio(portfolioId, alertId);
    }

    @Override
    @Transactional
    public PriceAlert update(Long portfolioId, Long alertId, Long assetId, PriceAlert changes) {
        PriceAlert alert = findInPortfolio(portfolioId, alertId);
        // alert ที่แจ้งเตือนไปแล้วหรือหมดอายุคือประวัติ ถ้าแก้ได้จะไม่รู้ว่าเคยแจ้งเตือนเงื่อนไขไหน
        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new IllegalStateException("แก้ได้เฉพาะ alert ที่ยังรอตรวจ (PENDING) — alert นี้สถานะ "
                    + alert.getStatus() + " แล้ว ให้สร้าง alert ใหม่แทน");
        }
        alert.setAsset(findAsset(assetId));
        alert.setCondition(changes.getCondition());
        alert.setTargetPrice(changes.getTargetPrice());
        return priceAlertRepository.save(alert);
    }

    @Override
    @Transactional
    public void delete(Long portfolioId, Long alertId) {
        priceAlertRepository.delete(findInPortfolio(portfolioId, alertId));
    }

    // alert ของพอร์ตอื่นตอบเหมือน "ไม่มี" (404) ไม่บอกว่ามีอยู่จริง
    private PriceAlert findInPortfolio(Long portfolioId, Long alertId) {
        return priceAlertRepository.findById(alertId)
                .filter(a -> a.getPortfolio().getId().equals(portfolioId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
    }

    private Asset findAsset(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
    }
}
