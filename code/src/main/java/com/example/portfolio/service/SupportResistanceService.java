package com.example.portfolio.service;

import com.example.portfolio.service.analysis.SupportResistanceLevels;

import java.time.LocalDate;

// ฟีเจอร์ 2: แนวรับ-แนวต้าน จากราคาย้อนหลังในช่วงวันที่ที่เลือก
public interface SupportResistanceService {

    // method = ชื่อ SupportResistanceStrategy: pivot (Pivot Point), ma (Moving Average Band)
    SupportResistanceLevels calculate(Long assetId, LocalDate from, LocalDate to, String method);
}
