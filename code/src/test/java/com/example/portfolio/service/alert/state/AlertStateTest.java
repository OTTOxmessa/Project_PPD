package com.example.portfolio.service.alert.state;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.service.alert.AlertPublisher;
import com.example.portfolio.service.alert.condition.PriceAboveEvaluator;
import com.example.portfolio.service.alert.condition.PriceBelowEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

// ทดสอบ State pattern ของ PriceAlert (PENDING -> TRIGGERED -> NOTIFIED) และ Strategy ของเงื่อนไขราคา
@ExtendWith(MockitoExtension.class)
class AlertStateTest {

    @Mock
    private AlertPublisher alertPublisher;

    private final PendingState pendingState =
            new PendingState(List.of(new PriceAboveEvaluator(), new PriceBelowEvaluator()), 90);

    private static PriceAlert alert(AlertCondition condition, String target) {
        return PriceAlert.builder()
                .id(1L)
                .asset(Asset.builder().id(1L).symbol("PTT").build())
                .condition(condition)
                .targetPrice(new BigDecimal(target))
                .build();
    }

    @ParameterizedTest(name = "{0} เป้า {1} ราคาปัจจุบัน {2} → {3}")
    @CsvSource({
            "PRICE_ABOVE, 100, 105,   TRIGGERED",
            "PRICE_ABOVE, 100, 100,   TRIGGERED",
            "PRICE_ABOVE, 100, 99.99, PENDING",
            "PRICE_BELOW, 30,  29.5,  TRIGGERED",
            "PRICE_BELOW, 30,  30,    TRIGGERED",
            "PRICE_BELOW, 30,  30.01, PENDING"
    })
    @DisplayName("PendingState: เปลี่ยนเป็น TRIGGERED เมื่อราคาถึงเงื่อนไขเท่านั้น")
    void pendingState(AlertCondition condition, String target, String price, AlertStatus expected) {
        PriceAlert alert = alert(condition, target);

        pendingState.handle(alert, new BigDecimal(price));

        assertThat(alert.getStatus()).isEqualTo(expected);
    }

    @Test
    @DisplayName("PendingState: เงื่อนไขที่ไม่มี evaluator → แจ้งชัดเจน ไม่เงียบ")
    void pendingStateWithoutEvaluator() {
        PendingState onlyAbove = new PendingState(List.of(new PriceAboveEvaluator()), 90);

        assertThatThrownBy(() -> onlyAbove.handle(alert(AlertCondition.PRICE_BELOW, "10"), BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("alert ใหม่เริ่มที่สถานะ PENDING")
    void newAlertStartsPending() {
        assertThat(alert(AlertCondition.PRICE_ABOVE, "1").getStatus()).isEqualTo(AlertStatus.PENDING);
    }

    @Test
    @DisplayName("TriggeredState: ประกาศผ่าน AlertPublisher แล้วเปลี่ยนเป็น NOTIFIED")
    void triggeredStateNotifies() {
        PriceAlert alert = alert(AlertCondition.PRICE_ABOVE, "100");
        alert.setStatus(AlertStatus.TRIGGERED);
        BigDecimal price = new BigDecimal("101");

        new TriggeredState(alertPublisher).handle(alert, price);

        verify(alertPublisher).publish(alert, price);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.NOTIFIED);
    }

    @Test
    @DisplayName("ExpiredState: สถานะสุดท้าย ไม่เปลี่ยนอะไรอีก")
    void expiredStateIsTerminal() {
        PriceAlert alert = alert(AlertCondition.PRICE_BELOW, "10");
        alert.setStatus(AlertStatus.EXPIRED);

        new ExpiredState().handle(alert, new BigDecimal("1"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.EXPIRED);
    }

    @Test
    @DisplayName("แต่ละ state บอกสถานะที่ตัวเองรับผิดชอบ (ใช้เลือก state แทน if-else)")
    void statesDeclareTheirStatus() {
        assertThat(pendingState.status()).isEqualTo(AlertStatus.PENDING);
        assertThat(new TriggeredState(alertPublisher).status()).isEqualTo(AlertStatus.TRIGGERED);
        assertThat(new ExpiredState().status()).isEqualTo(AlertStatus.EXPIRED);
    }

    @Test
    @DisplayName("PendingState: รอนานเกินกำหนดโดยราคาไม่ถึงเป้า → EXPIRED")
    void pendingAlertExpires() {
        PriceAlert alert = alert(AlertCondition.PRICE_ABOVE, "100");
        alert.setCreatedAt(LocalDateTime.now().minusDays(91));

        pendingState.handle(alert, new BigDecimal("80"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.EXPIRED);
    }

    @Test
    @DisplayName("PendingState: alert เก่าแต่ราคาถึงเป้าพอดี → ยังแจ้งเตือน (TRIGGERED) ไม่ใช่หมดอายุ")
    void oldAlertStillTriggers() {
        PriceAlert alert = alert(AlertCondition.PRICE_ABOVE, "100");
        alert.setCreatedAt(LocalDateTime.now().minusDays(200));

        pendingState.handle(alert, new BigDecimal("101"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.TRIGGERED);
    }

    @Test
    @DisplayName("PendingState: alert อายุยังไม่ถึงกำหนด → ยัง PENDING")
    void recentAlertStaysPending() {
        PriceAlert alert = alert(AlertCondition.PRICE_ABOVE, "100");
        alert.setCreatedAt(LocalDateTime.now().minusDays(10));

        pendingState.handle(alert, new BigDecimal("80"));

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.PENDING);
    }
}
