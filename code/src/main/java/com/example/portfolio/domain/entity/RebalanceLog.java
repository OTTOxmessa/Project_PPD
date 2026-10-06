package com.example.portfolio.domain.entity;

import com.example.portfolio.domain.enums.RebalanceMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rebalance_logs", indexes = @Index(name = "idx_rebalance_logs_portfolio_id", columnList = "portfolio_id, triggered_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RebalanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rebalance_logs_portfolio_id"))
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RebalanceMethod method;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    // เก็บ target vs actual allocation + รายการ trade ที่เกิดขึ้นเป็น JSON string
    // เลือกวิธีนี้แทนการสร้างตารางลูก (rebalance_log_items) เพื่อไม่ให้ schema ซับซ้อนเกินจำเป็น
    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        this.triggeredAt = LocalDateTime.now();
    }
}
