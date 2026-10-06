package com.example.portfolio.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uq_users_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 1:1 — profile มีความหมายเฉพาะเมื่อผูกกับ user จึง cascade ALL + orphanRemoval
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    // 1:N — ลบ user แล้วพอร์ตทั้งหมดต้องลบตาม
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Portfolio> portfolios = new ArrayList<>();

    // N:M — Watchlist ของผู้ใช้ (หุ้นหนึ่งตัวอยู่ใน watchlist ได้หลายคน, คนหนึ่งมีได้หลายตัว)
    // ใช้ @ManyToMany ตรง ๆ ได้เพราะตาราง user_watchlist ไม่มีข้อมูลอื่นนอกจาก 2 FK
    // (ต่างจาก holdings ที่ต้องเก็บ quantity/avgCost จึงต้องแตกเป็น entity แยก)
    // ไม่มี cascade: เอาหุ้นออกจาก watchlist ต้องไม่ลบตัว Asset
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_watchlist",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_watchlist_user_id")),
            inverseJoinColumns = @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "fk_user_watchlist_asset_id")),
            indexes = @Index(name = "idx_user_watchlist_asset_id", columnList = "asset_id"))
    @OrderBy("symbol ASC")
    @Builder.Default
    private Set<Asset> watchlist = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
