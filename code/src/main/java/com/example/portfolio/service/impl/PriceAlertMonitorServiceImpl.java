package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.repository.PriceAlertRepository;
import com.example.portfolio.service.PriceAlertMonitorService;
import com.example.portfolio.service.alert.state.AlertState;
import com.example.portfolio.service.market.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// State pattern ตัวจริง: เลือก AlertState จากสถานะปัจจุบันของ alert แล้วให้ state จัดการ
// ทำต่อจนสถานะไม่เปลี่ยนอีก (PENDING -> TRIGGERED -> NOTIFIED ได้ในรอบเดียว)
// ไม่มี if-else ตามสถานะ — เพิ่มสถานะใหม่ = เพิ่มคลาส AlertState หนึ่งคลาส (OCP)
@Service
public class PriceAlertMonitorServiceImpl implements PriceAlertMonitorService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertMonitorServiceImpl.class);

    private final PriceAlertRepository priceAlertRepository;
    private final MarketDataProvider marketDataProvider;
    private final Map<AlertStatus, AlertState> states = new EnumMap<>(AlertStatus.class);

    public PriceAlertMonitorServiceImpl(PriceAlertRepository priceAlertRepository,
                                        MarketDataProvider marketDataProvider,
                                        List<AlertState> states) {
        this.priceAlertRepository = priceAlertRepository;
        this.marketDataProvider = marketDataProvider;
        for (AlertState state : states) {
            this.states.put(state.status(), state);
        }
    }

    @Override
    @Transactional
    public void checkAndNotify(PriceAlert alert, BigDecimal currentPrice) {
        AlertStatus initial = alert.getStatus();
        AlertState state = states.get(alert.getStatus());
        while (state != null) {
            AlertStatus before = alert.getStatus();
            state.handle(alert, currentPrice);
            if (alert.getStatus() == before) {
                break; // state ไม่เปลี่ยนสถานะแล้ว
            }
            state = states.get(alert.getStatus());
        }
        if (alert.getStatus() != initial) {
            priceAlertRepository.save(alert);
        }
    }

    // @Transactional จำเป็น: เรียกจาก scheduler ซึ่งไม่มี request/session เหมือน controller
    // ถ้าไม่มี การอ่าน alert.getAsset().getSymbol() (lazy) จะเกิด LazyInitializationException
    @Override
    @Transactional
    public int checkPendingAlerts() {
        List<PriceAlert> pendingAlerts = priceAlertRepository.findByStatus(AlertStatus.PENDING);
        for (PriceAlert alert : pendingAlerts) {
            try {
                BigDecimal currentPrice = marketDataProvider.getLatestPrice(alert.getAsset().getSymbol());
                checkAndNotify(alert, currentPrice);
            } catch (Exception e) {
                // alert ตัวหนึ่งพัง (เช่น ยังไม่มีราคา) ต้องไม่ทำให้ตัวอื่นไม่ถูกตรวจ
                log.error("ตรวจสอบ alert #{} ไม่สำเร็จ: {}", alert.getId(), e.getMessage());
            }
        }
        return pendingAlerts.size();
    }
}
