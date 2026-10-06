package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_history",
        // unique (asset_id, price_date) เป็น index สำหรับค้นราคาตามช่วงวันที่ในตัว ไม่ต้องสร้าง index ซ้ำ
        uniqueConstraints = @UniqueConstraint(name = "uq_price_history_asset_date", columnNames = {"asset_id", "price_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_price_history_asset_id"))
    private Asset asset;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal close;

    private Long volume;
}
