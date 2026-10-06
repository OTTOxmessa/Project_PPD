package com.example.portfolio.repository;

import com.example.portfolio.domain.entity.RebalanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RebalanceLogRepository extends JpaRepository<RebalanceLog, Long> {

    List<RebalanceLog> findByPortfolioIdOrderByTriggeredAtDesc(Long portfolioId);

    Page<RebalanceLog> findByPortfolioId(Long portfolioId, Pageable pageable);
}
