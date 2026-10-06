package com.example.portfolio.service.alert;

import com.example.portfolio.domain.entity.PriceAlert;

import java.math.BigDecimal;

// Observer pattern: ผู้รับการแจ้งเตือนแต่ละช่องทาง implement interface นี้
public interface AlertObserver {

    void onAlertTriggered(PriceAlert alert, BigDecimal currentPrice);
}
