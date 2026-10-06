package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.AllocationTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllocationTargetRepository extends JpaRepository<AllocationTarget, Long> {

    List<AllocationTarget> findByPortfolioId(Long portfolioId);

    Optional<AllocationTarget> findByPortfolioIdAndAssetId(Long portfolioId, Long assetId);
}
