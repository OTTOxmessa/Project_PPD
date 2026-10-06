package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.IndexPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndexPriceHistoryRepository extends JpaRepository<IndexPriceHistory, Long> {

    List<IndexPriceHistory> findByMarketIndexIdAndPriceDateBetweenOrderByPriceDateAsc(
            Long marketIndexId, LocalDate start, LocalDate end);

    Optional<IndexPriceHistory> findTopByMarketIndexIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
            Long marketIndexId, LocalDate date);

    @Modifying
    @Query("delete from IndexPriceHistory i where i.marketIndex.id = :marketIndexId")
    int purgeByMarketIndexId(@Param("marketIndexId") Long marketIndexId);
}
