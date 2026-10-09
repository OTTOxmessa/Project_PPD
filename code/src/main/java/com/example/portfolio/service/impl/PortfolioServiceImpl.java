package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.User;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.UserRepository;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.market.UsMarket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // DIP: inject ผ่าน constructor เท่านั้น (Lombok generate constructor จาก final field ให้)
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Portfolio create(Long userId, Portfolio portfolio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        portfolio.setUser(user);
        portfolio.setBaseCurrency(UsMarket.CURRENCY); // ระบบรองรับเฉพาะ USD (request อื่นถูกปฏิเสธที่ DTO แล้ว)
        return portfolioRepository.save(portfolio);
    }

    @Override
    public Portfolio getByIdForUser(Long id, Long userId) {
        // findByIdAndUserId กัน user คนอื่นเห็น/แก้พอร์ตที่ไม่ใช่ของตัวเอง
        return portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));
    }

    @Override
    public Page<Portfolio> listForUser(Long userId, Pageable pageable) {
        return portfolioRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional
    public Portfolio update(Long id, Long userId, Portfolio updates) {
        Portfolio existing = getByIdForUser(id, userId);
        existing.setName(updates.getName());
        existing.setBaseCurrency(UsMarket.CURRENCY);
        return portfolioRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Portfolio existing = getByIdForUser(id, userId);
        portfolioRepository.delete(existing);
    }
}
