package com.example.portfolio.scheduler;

import com.example.portfolio.service.PriceAlertMonitorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// จุดเริ่มงานตามเวลา — ตรรกะการตรวจ alert ทั้งหมดอยู่ใน PriceAlertMonitorService
@Component
@RequiredArgsConstructor
public class PriceAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertScheduler.class);

    private final PriceAlertMonitorService priceAlertMonitorService;

    // ความถี่ตั้งผ่าน property alert.check-interval-ms (default 5 นาที, dev ตั้งเป็น 30 วินาที)
    @Scheduled(fixedRateString = "${alert.check-interval-ms:300000}", initialDelay = 10000)
    public void checkPendingAlerts() {
        int checked = priceAlertMonitorService.checkPendingAlerts();
        log.debug("ตรวจ price alert ที่รออยู่ {} รายการ", checked);
    }
}
