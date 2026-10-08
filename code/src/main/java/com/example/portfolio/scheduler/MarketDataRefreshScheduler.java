package com.example.portfolio.scheduler;

import com.example.portfolio.service.MarketDataRefreshService;
import com.example.portfolio.service.market.RefreshSummary;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// จุดเริ่มงานตามเวลา (เทียบเท่า controller แต่ถูกเรียกด้วยนาฬิกา) — สั่ง service อย่างเดียว
// อัปเดตราคาจริงจาก Yahoo: ครั้งแรก 5 วินาทีหลังแอป start แล้วทุก 6 ชั่วโมง (ปรับได้ที่ market-data.refresh-interval-ms)
@Component
@RequiredArgsConstructor
public class MarketDataRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshScheduler.class);

    private final MarketDataRefreshService marketDataRefreshService;

    @Scheduled(initialDelay = 5000, fixedDelayString = "${market-data.refresh-interval-ms:21600000}")
    public void refreshAll() {
        RefreshSummary result = marketDataRefreshService.refreshAll();
        log.info("อัปเดตราคาจริงสำเร็จ {}/{} สินทรัพย์, {}/{} ดัชนี (ที่เหลือใช้ข้อมูลเดิม)",
                result.assetsUpdated(), result.assetsTotal(), result.indicesUpdated(), result.indicesTotal());
    }
}
