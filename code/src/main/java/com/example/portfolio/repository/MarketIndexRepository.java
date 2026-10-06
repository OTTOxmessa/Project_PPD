package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.MarketIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketIndexRepository extends JpaRepository<MarketIndex, Long> {

    Optional<MarketIndex> findByIndexCode(String indexCode);
}
