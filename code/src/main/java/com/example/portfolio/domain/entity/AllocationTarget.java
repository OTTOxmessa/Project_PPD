package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "allocation_targets",
        uniqueConstraints = @UniqueConstraint(name = "uq_allocation_targets_portfolio_asset", columnNames = {"portfolio_id", "asset_id"}),
        indexes = @Index(name = "idx_allocation_targets_asset_id", columnList = "asset_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_allocation_targets_portfolio_id"))
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_allocation_targets_asset_id"))
    private Asset asset;

    @Column(name = "target_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetPercent; // เช่น 25.00 หมายถึง 25%
}
