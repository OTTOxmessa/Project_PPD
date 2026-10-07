package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.PriceAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบการจัดการ alert ที่ผู้ใช้ตั้ง (CRUD)
@ExtendWith(MockitoExtension.class)
class PriceAlertServiceImplTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private PriceAlertServiceImpl service;

    private static PriceAlert alertInPortfolio5(AlertStatus status) {
        return PriceAlert.builder().id(1L).portfolio(Portfolio.builder().id(5L).build())
                .asset(Asset.builder().id(1L).symbol("PTT").build())
                .condition(AlertCondition.PRICE_BELOW).targetPrice(new BigDecimal("30")).status(status).build();
    }

    @Test
    @DisplayName("create: ผูกพอร์ต/สินทรัพย์ และบังคับสถานะเริ่มต้นเป็น PENDING")
    void createForcesPending() {
        Portfolio portfolio = Portfolio.builder().id(5L).build();
        Asset asset = Asset.builder().id(1L).symbol("PTT").build();
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(portfolio));
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenAnswer(inv -> inv.getArgument(0));
        PriceAlert input = PriceAlert.builder().condition(AlertCondition.PRICE_BELOW)
                .targetPrice(new BigDecimal("30")).status(AlertStatus.NOTIFIED).build();

        PriceAlert saved = service.create(5L, 1L, input);

        assertThat(saved.getStatus()).isEqualTo(AlertStatus.PENDING);
        assertThat(saved.getPortfolio()).isSameAs(portfolio);
        assertThat(saved.getAsset()).isSameAs(asset);
    }

    @Test
    @DisplayName("create: สินทรัพย์ไม่มีอยู่ → ResourceNotFoundException ไม่บันทึก")
    void createUnknownAsset() {
        when(portfolioRepository.findById(5L)).thenReturn(Optional.of(Portfolio.builder().id(5L).build()));
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(5L, 99L, new PriceAlert()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById: alert ของพอร์ตอื่น → ResourceNotFoundException (ไม่บอกว่ามีอยู่จริง)")
    void getByIdOfOtherPortfolio() {
        when(priceAlertRepository.findById(1L)).thenReturn(Optional.of(alertInPortfolio5(AlertStatus.PENDING)));

        assertThatThrownBy(() -> service.getById(999L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("update: alert ที่ยัง PENDING → แก้สินทรัพย์/เงื่อนไข/ราคาเป้าหมายได้")
    void updatePending() {
        PriceAlert alert = alertInPortfolio5(AlertStatus.PENDING);
        Asset aot = Asset.builder().id(2L).symbol("AOT").build();
        when(priceAlertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(assetRepository.findById(2L)).thenReturn(Optional.of(aot));
        when(priceAlertRepository.save(any(PriceAlert.class))).thenAnswer(inv -> inv.getArgument(0));
        PriceAlert changes = PriceAlert.builder().condition(AlertCondition.PRICE_ABOVE)
                .targetPrice(new BigDecimal("70")).build();

        PriceAlert saved = service.update(5L, 1L, 2L, changes);

        assertThat(saved.getAsset()).isSameAs(aot);
        assertThat(saved.getCondition()).isEqualTo(AlertCondition.PRICE_ABOVE);
        assertThat(saved.getTargetPrice()).isEqualByComparingTo("70");
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.PENDING);
    }

    @Test
    @DisplayName("update: alert ที่แจ้งเตือนไปแล้ว → IllegalStateException (409) ไม่บันทึก")
    void updateNotifiedRejected() {
        when(priceAlertRepository.findById(1L)).thenReturn(Optional.of(alertInPortfolio5(AlertStatus.NOTIFIED)));

        assertThatThrownBy(() -> service.update(5L, 1L, 1L, new PriceAlert()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOTIFIED");
        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: alert ของพอร์ตอื่น → ResourceNotFoundException ไม่ลบ")
    void deleteAlertOfOtherPortfolio() {
        when(priceAlertRepository.findById(1L)).thenReturn(Optional.of(alertInPortfolio5(AlertStatus.PENDING)));

        assertThatThrownBy(() -> service.delete(999L, 1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(priceAlertRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: alert ของพอร์ตตัวเอง → ลบได้")
    void deleteOwnAlert() {
        PriceAlert alert = alertInPortfolio5(AlertStatus.PENDING);
        when(priceAlertRepository.findById(1L)).thenReturn(Optional.of(alert));

        service.delete(5L, 1L);

        verify(priceAlertRepository).delete(alert);
    }
}
