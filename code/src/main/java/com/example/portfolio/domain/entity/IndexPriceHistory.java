package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "index_price_history",
        // unique (market_index_id, price_date) เป็น index สำหรับค้นตามช่วงวันที่ในตัว ไม่ต้องสร้าง index ซ้ำ
        uniqueConstraints = @UniqueConstraint(name = "uq_index_price_history_index_date", columnNames = {"market_index_id", "price_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_index_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_index_price_history_market_index_id"))
    private MarketIndex marketIndex;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "close_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal closeValue;
}
