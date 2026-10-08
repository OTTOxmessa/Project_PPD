package com.example.portfolio.service.allocation;

import com.example.portfolio.common.NamedStrategy;
import com.example.portfolio.domain.entity.Holding;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Strategy pattern: สลับวิธีคำนวณสัดส่วนสินทรัพย์ปัจจุบันได้โดยไม่ต้องแก้ AllocationService (OCP)
public interface AllocationStrategy extends NamedStrategy {

    // คืนค่า Map<assetId, สัดส่วนปัจจุบันเป็น %> จาก holding ที่มีอยู่ + ราคาล่าสุดของแต่ละ asset
    Map<Long, BigDecimal> calculateCurrentAllocation(List<Holding> holdings, Map<Long, BigDecimal> latestPrices);
}
