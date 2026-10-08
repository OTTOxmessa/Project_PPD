package com.example.portfolio.service;

import com.example.portfolio.service.market.RefreshSummary;

// อัปเดตราคาจริงของทุกสินทรัพย์และทุกดัชนีในระบบ (MarketDataRefreshScheduler เรียก)
// แยกจาก PriceHistoryService เพราะผู้เรียกต่างกันและใช้คนละเมธอด (ISP)
public interface MarketDataRefreshService {

    RefreshSummary refreshAll();
}
