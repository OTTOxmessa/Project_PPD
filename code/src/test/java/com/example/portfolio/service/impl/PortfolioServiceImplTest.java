package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.User;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบ CRUD ของพอร์ต และการกันไม่ให้ผู้ใช้คนอื่นเข้าถึงพอร์ตที่ไม่ใช่ของตัวเอง
@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    private static User user(Long id) {
        return User.builder().id(id).username("otto").email("otto@example.com").build();
    }

    private static Portfolio portfolio(Long id, User owner) {
        return Portfolio.builder().id(id).user(owner).name("Long-term").baseCurrency("THB").build();
    }

    @Test
    @DisplayName("create: ผูกพอร์ตใหม่เข้ากับผู้ใช้ก่อนบันทึก")
    void createAssignsOwner() {
        User owner = user(1L);
        Portfolio input = Portfolio.builder().name("Dividend").baseCurrency("THB").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));

        Portfolio saved = portfolioService.create(1L, input);

        assertThat(saved.getUser()).isSameAs(owner);
        assertThat(saved.getName()).isEqualTo("Dividend");
    }

    @Test
    @DisplayName("create: ผู้ใช้ไม่มีอยู่จริง → ResourceNotFoundException และไม่บันทึก")
    void createUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.create(99L, new Portfolio()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByIdForUser: พอร์ตของคนอื่น → ResourceNotFoundException (ไม่บอกว่ามีอยู่)")
    void getByIdForOtherUser() {
        when(portfolioRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getByIdForUser(5L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("listForUser: ส่ง Pageable ต่อให้ repository ตรง ๆ (pagination & sorting)")
    void listForUserPassesPageable() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Portfolio> page = new PageImpl<>(List.of(portfolio(1L, user(1L))), pageable, 6);
        when(portfolioRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<Portfolio> result = portfolioService.listForUser(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("update: แก้ได้เฉพาะชื่อและสกุลเงิน เจ้าของเดิมไม่เปลี่ยน")
    void updateChangesNameAndCurrency() {
        User owner = user(1L);
        Portfolio existing = portfolio(5L, owner);
        when(portfolioRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(existing));
        when(portfolioRepository.save(existing)).thenReturn(existing);

        Portfolio updates = Portfolio.builder().name("US Tech").baseCurrency("USD").build();
        Portfolio result = portfolioService.update(5L, 1L, updates);

        assertThat(result.getName()).isEqualTo("US Tech");
        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getUser()).isSameAs(owner);
    }

    @Test
    @DisplayName("delete: ลบเฉพาะพอร์ตที่เป็นของผู้ใช้")
    void deleteOwnPortfolio() {
        Portfolio existing = portfolio(5L, user(1L));
        when(portfolioRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(existing));

        portfolioService.delete(5L, 1L);

        verify(portfolioRepository).delete(existing);
    }

    @Test
    @DisplayName("delete: พอร์ตของคนอื่น → ไม่ลบอะไรเลย")
    void deleteOtherUsersPortfolio() {
        when(portfolioRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.delete(5L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(portfolioRepository, never()).delete(any());
    }
}
