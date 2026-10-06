package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "market_indices",
        uniqueConstraints = @UniqueConstraint(name = "uq_market_indices_code", columnNames = "index_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "index_code", nullable = false, length = 20)
    private String indexCode; // เช่น SET, SPX

    @Column(nullable = false, length = 100)
    private String name;
}
