package com.example.portfolio.service.impl;

import com.example.portfolio.common.StrategyRegistry;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.SupportResistanceService;
import com.example.portfolio.service.analysis.SupportResistanceLevels;
import com.example.portfolio.service.analysis.SupportResistanceStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

// ดึง price_history จาก DB แล้วส่งให้ SupportResistanceStrategy ที่ผู้ใช้เลือกคำนวณ
@Service
public class SupportResistanceServiceImpl implements SupportResistanceService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final StrategyRegistry<SupportResistanceStrategy> strategies;

    public SupportResistanceServiceImpl(PriceHistoryRepository priceHistoryRepository,
                                        List<SupportResistanceStrategy> strategies) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.strategies = new StrategyRegistry<>("คำนวณแนวรับ-แนวต้าน", strategies);
    }

    @Override
    public SupportResistanceLevels calculate(Long assetId, LocalDate from, LocalDate to, String method) {
        SupportResistanceStrategy strategy = strategies.get(method);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("วันเริ่มต้นต้องไม่อยู่หลังวันสิ้นสุด");
        }
        List<PriceHistory> history = priceHistoryRepository
                .findByAssetIdAndPriceDateBetweenOrderByPriceDateAsc(assetId, from, to);
        if (history.isEmpty()) {
            throw new IllegalStateException("ไม่มีข้อมูลราคาของ asset " + assetId + " ในช่วงเวลาที่ระบุ");
        }
        return strategy.calculate(history);
    }
}
