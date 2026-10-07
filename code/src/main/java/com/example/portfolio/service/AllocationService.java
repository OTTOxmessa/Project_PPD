package com.example.portfolio.service;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.service.allocation.AllocationComparison;

import java.math.BigDecimal;
import java.util.List;

// ฟีเจอร์ 1: Asset Allocation — เทียบสัดส่วนจริงกับเป้าหมาย และตั้งเป้าหมายรายสินทรัพย์
public interface AllocationService {

    // method = ชื่อ AllocationStrategy: target (ตามมูลค่าตลาด), equal (เท่ากันทุกตัว), risk (ถ่วงความเสี่ยง)
    List<AllocationComparison> compare(Long portfolioId, String method);

    List<AllocationTarget> getTargets(Long portfolioId);

    AllocationTarget upsertTarget(Long portfolioId, Long assetId, BigDecimal targetPercent);
}
