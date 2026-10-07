package com.example.portfolio.service;

import com.example.portfolio.domain.entity.PriceAlert;

import java.util.List;

// ฟีเจอร์ 3: Price Alert — ส่วนที่ผู้ใช้จัดการผ่านหน้าเว็บ (CRUD)
// การตรวจราคาและแจ้งเตือนแยกไปอยู่ที่ PriceAlertMonitorService (ISP: controller ไม่ต้องเห็นเมธอดของ scheduler)
// ทุกเมธอดรับ portfolioId ด้วย เพื่อกันการเข้าถึง alert ของพอร์ตอื่นด้วยการเดา alertId
public interface PriceAlertService {

    PriceAlert create(Long portfolioId, Long assetId, PriceAlert alert);

    List<PriceAlert> getByPortfolio(Long portfolioId);

    PriceAlert getById(Long portfolioId, Long alertId);

    // แก้ได้เฉพาะ alert ที่ยังรอตรวจ (PENDING)
    PriceAlert update(Long portfolioId, Long alertId, Long assetId, PriceAlert changes);

    void delete(Long portfolioId, Long alertId);
}
