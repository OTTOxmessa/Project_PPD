package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.repository.PriceAlertRepository;
import com.example.portfolio.service.alert.AlertObserver;
import com.example.portfolio.service.alert.AlertSubject;
import com.example.portfolio.service.alert.condition.PriceAboveEvaluator;
import com.example.portfolio.service.alert.condition.PriceBelowEvaluator;
import com.example.portfolio.service.alert.state.ExpiredState;
import com.example.portfolio.service.alert.state.PendingState;
import com.example.portfolio.service.alert.state.TriggeredState;
import com.example.portfolio.service.market.MarketDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบ State + Observer ทำงานร่วมกัน (ใช้ state และ evaluator จริง, mock แค่ repository, ราคา และ observer)
@ExtendWith(MockitoExtension.class)
class PriceAlertMonitorServiceImplTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;
    @Mock
    private MarketDataProvider marketDataProvider;
    @Mock
    private AlertObserver emailObserver;
    @Mock
    private AlertObserver inAppObserver;

    private PriceAlertMonitorServiceImpl monitor;

    @BeforeEach
    void setUp() {
        AlertSubject subject = new AlertSubject(List.of(emailObserver, inAppObserver));
        monitor = new PriceAlertMonitorServiceImpl(priceAlertRepository, marketDataProvider, List.of(
                new PendingState(List.of(new PriceAboveEvaluator(), new PriceBelowEvaluator()), 90),
                new TriggeredState(subject),
                new ExpiredState()));
    }

    private static PriceAlert pendingAbove(long id, String symbol, String target) {
        return PriceAlert.builder()
                .id(id)
                .portfolio(Portfolio.builder().id(5L).build())
                .asset(Asset.builder().id(id).symbol(symbol).build())
                .condition(AlertCondition.PRICE_ABOVE)
                .targetPrice(new BigDecimal(target))
                .build();
    }

    @Test
    @DisplayName("ราคาถึงเป้า → PENDING → TRIGGERED → NOTIFIED ในรอบเดียว แจ้งทุก observer และบันทึก")
    void triggersAndNotifiesAllObservers() {
        PriceAlert alert = pendingAbove(1L, "PTT", "100");
        BigDecimal price = new BigDecimal("120");

        monitor.checkAndNotify(alert, price);

        verify(emailObserver).onAlertTriggered(alert, price);
        verify(inAppObserver).onAlertTriggered(alert, price);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.NOTIFIED);
        verify(priceAlertRepository).save(alert);
    }

    @Test
    @DisplayName("ราคายังไม่ถึงเป้า → ไม่แจ้ง สถานะยัง PENDING และไม่ต้องบันทึก")
    void notTriggeredYet() {
        PriceAlert alert = pendingAbove(1L, "PTT", "100");

        monitor.checkAndNotify(alert, new BigDecimal("80"));

        verifyNoInteractions(emailObserver, inAppObserver);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.PENDING);
        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("alert ที่แจ้งไปแล้ว / หมดอายุ → ไม่แจ้งซ้ำและไม่บันทึกซ้ำ")
    void finishedAlertsAreIgnored() {
        PriceAlert notified = pendingAbove(1L, "PTT", "100");
        notified.setStatus(AlertStatus.NOTIFIED);
        PriceAlert expired = pendingAbove(2L, "AOT", "100");
        expired.setStatus(AlertStatus.EXPIRED);

        monitor.checkAndNotify(notified, new BigDecimal("150"));
        monitor.checkAndNotify(expired, new BigDecimal("150"));

        verifyNoInteractions(emailObserver, inAppObserver);
        verify(priceAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkPendingAlerts: ตรวจทุก alert ที่รออยู่ ตัวที่ดึงราคาไม่ได้ไม่ทำให้ตัวอื่นหยุด")
    void checkPendingAlertsContinuesAfterFailure() {
        PriceAlert broken = pendingAbove(1L, "NOPRICE", "10");
        PriceAlert ready = pendingAbove(2L, "AOT", "60");
        when(priceAlertRepository.findByStatus(AlertStatus.PENDING)).thenReturn(List.of(broken, ready));
        when(marketDataProvider.getLatestPrice("NOPRICE")).thenThrow(new IllegalStateException("ยังไม่มีราคา"));
        when(marketDataProvider.getLatestPrice("AOT")).thenReturn(new BigDecimal("61"));

        int checked = monitor.checkPendingAlerts();

        assertThat(checked).isEqualTo(2);
        assertThat(broken.getStatus()).isEqualTo(AlertStatus.PENDING);
        assertThat(ready.getStatus()).isEqualTo(AlertStatus.NOTIFIED);
        verify(emailObserver).onAlertTriggered(ready, new BigDecimal("61"));
    }

    @Test
    @DisplayName("AlertSubject: เพิ่ม observer ใหม่ได้โดยไม่แก้โค้ดเดิม (แจ้งครบทุกตัวในลิสต์)")
    void subjectNotifiesEveryObserver() {
        AlertObserver sms = mock(AlertObserver.class);
        PriceAlert alert = pendingAbove(1L, "PTT", "1");
        new AlertSubject(List.of(emailObserver, sms)).publish(alert, BigDecimal.TEN);

        verify(emailObserver).onAlertTriggered(alert, BigDecimal.TEN);
        verify(sms).onAlertTriggered(alert, BigDecimal.TEN);
        verifyNoInteractions(inAppObserver);
    }

    @Test
    @DisplayName("alert ที่รอนานเกินกำหนด → PENDING → EXPIRED บันทึก และไม่แจ้งเตือน")
    void staleAlertExpires() {
        PriceAlert alert = pendingAbove(1L, "PTT", "100");
        alert.setCreatedAt(LocalDateTime.now().minusDays(120));

        monitor.checkAndNotify(alert, new BigDecimal("50"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.EXPIRED);
        verifyNoInteractions(emailObserver, inAppObserver);
        verify(priceAlertRepository).save(alert);
    }
}
