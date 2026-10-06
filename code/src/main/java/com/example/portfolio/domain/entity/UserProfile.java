package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles",
        // unique บน user_id คือสิ่งที่ทำให้เป็นความสัมพันธ์ 1:1 ในระดับฐานข้อมูล
        uniqueConstraints = @UniqueConstraint(name = "uq_user_profiles_user_id", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1:1 — FK อยู่ฝั่งนี้ (owning side), uq_user_profiles_user_id ใน @Table บังคับว่า user หนึ่งคนมีได้แค่ 1 profile
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_profiles_user_id"))
    private User user;

    @Column(name = "risk_tolerance", length = 20)
    private String riskTolerance; // LOW / MEDIUM / HIGH

    @Column(name = "investment_goal", length = 255)
    private String investmentGoal;

    @Column(length = 20)
    private String phone;
}
