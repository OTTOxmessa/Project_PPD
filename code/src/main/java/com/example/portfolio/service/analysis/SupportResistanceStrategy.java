package com.example.portfolio.service.analysis;

import com.example.portfolio.common.NamedStrategy;
import com.example.portfolio.domain.entity.PriceHistory;

import java.util.List;

// Strategy pattern: สลับวิธีคำนวณแนวรับ-แนวต้านได้โดยไม่ต้องแก้ SupportResistanceService (OCP)
public interface SupportResistanceStrategy extends NamedStrategy {

    SupportResistanceLevels calculate(List<PriceHistory> priceHistory);
}
