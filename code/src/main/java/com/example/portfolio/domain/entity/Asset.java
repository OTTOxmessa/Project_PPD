package com.example.portfolio.domain.entity;

import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.domain.enums.PriceSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assets",
        uniqueConstraints = @UniqueConstraint(name = "uq_assets_symbol", columnNames = "symbol"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol; // ticker ตลาดสหรัฐฯ เช่น AAPL, BRK.B, VOO

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(length = 50)
    private String exchange; // "US" (ระบบรองรับเฉพาะตลาดสหรัฐฯ)

    // ราคาใน price_history มาจากไหน (null = ยังไม่เคยนำเข้า/ข้อมูลเก่าก่อนมีฟิลด์นี้)
    @Enumerated(EnumType.STRING)
    @Column(name = "price_source", length = 20)
    private PriceSource priceSource;

    // ตั้งใจไม่ใส่ @OneToMany ย้อนกลับไปหา Holding/Transaction/PriceHistory
    // เพราะ Asset เป็น reference data ที่ถูกหลายฝั่งอ้างถึง ไม่ใช่ aggregate root
}
