package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// body ของ PUT /api/v1/assets/by-symbol/{symbol} (symbol อยู่ใน URL)
// name/assetType/exchange ไม่บังคับถ้า symbol อยู่ในรายชื่อหุ้นอ้างอิง
// referencePrice = ราคาล่าสุดสำหรับสร้างข้อมูลราคาจำลอง (ใช้เฉพาะสินทรัพย์ที่ยังไม่มีราคา)
public record EnsureAssetRequest(
        @Size(max = 100) String name,
        AssetType assetType,
        @Size(max = 50) String exchange,
        @DecimalMin(value = "0", inclusive = false) BigDecimal referencePrice
) {
}
