package com.example.portfolio.domain.entity;

import com.example.portfolio.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        // ดูประวัติของพอร์ตและคำนวณผลตอบแทนตามช่วงเวลา ใช้ (portfolio_id, executed_at) คู่กันเสมอ
        @Index(name = "idx_transactions_portfolio_executed", columnList = "portfolio_id, executed_at"),
        @Index(name = "idx_transactions_asset_id", columnList = "asset_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_portfolio_id"))
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_transactions_asset_id"))
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
