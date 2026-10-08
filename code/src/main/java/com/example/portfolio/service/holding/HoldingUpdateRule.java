package com.example.portfolio.service.holding;

import com.example.portfolio.domain.entity.Holding;
import com.example.portfolio.domain.enums.TransactionType;

import java.math.BigDecimal;

// Strategy pattern: วิธีปรับ Holding ตามประเภทธุรกรรม — HoldingService เลือก rule จาก type()
// เพิ่มประเภทที่กระทบ holding = เพิ่ม rule หนึ่งคลาส ไม่ต้องแก้ switch เดิม (OCP)
public interface HoldingUpdateRule {

    TransactionType type();

    void apply(Holding holding, BigDecimal quantity, BigDecimal price);
}
