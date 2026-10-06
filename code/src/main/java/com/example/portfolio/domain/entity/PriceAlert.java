package com.example.portfolio.domain.entity;

import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts", indexes = {
        // scheduler จะ query ด้วย status="PENDING" เป็นหลักทุกรอบ ต้องมี index ตัวนี้
        @Index(name = "idx_price_alerts_status", columnList = "status"),
        @Index(name = "idx_price_alerts_portfolio_id", columnList = "portfolio_id"),
        @Index(name = "idx_price_alerts_asset_id", columnList = "asset_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_price_alerts_portfolio_id"))
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_price_alerts_asset_id"))
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertCondition condition;

    @Column(name = "target_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
