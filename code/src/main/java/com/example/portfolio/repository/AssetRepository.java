package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findBySymbol(String symbol);

    List<Asset> findByAssetType(AssetType assetType);

    boolean existsBySymbol(String symbol);

    // สินทรัพย์ยังถูกใช้อยู่ไหม (ตารางที่ FK เป็น ON DELETE RESTRICT) — ใช้ EXISTS จึงหยุดทันทีที่เจอแถวแรก
    // Asset ไม่มี @OneToMany ย้อนกลับ (ตั้งใจ) จึงเขียนเป็น native query
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM holdings WHERE asset_id = :assetId)
                OR EXISTS (SELECT 1 FROM transactions WHERE asset_id = :assetId)
                OR EXISTS (SELECT 1 FROM price_alerts WHERE asset_id = :assetId)
                OR EXISTS (SELECT 1 FROM allocation_targets WHERE asset_id = :assetId)
            """, nativeQuery = true)
    boolean isInUse(@Param("assetId") Long assetId);

    // user_watchlist เป็นตารางเชื่อมของ @ManyToMany (ไม่มี entity ของตัวเอง)
    @Modifying
    @Query(value = "DELETE FROM user_watchlist WHERE asset_id = :assetId", nativeQuery = true)
    int removeFromAllWatchlists(@Param("assetId") Long assetId);
}
