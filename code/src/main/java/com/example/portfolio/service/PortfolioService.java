package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PortfolioService {

    Portfolio create(Long userId, Portfolio portfolio);

    Portfolio getByIdForUser(Long id, Long userId);

    Page<Portfolio> listForUser(Long userId, Pageable pageable);

    Portfolio update(Long id, Long userId, Portfolio updates);

    void delete(Long id, Long userId);
}
