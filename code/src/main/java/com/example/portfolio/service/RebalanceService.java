package com.example.portfolio.service;

import com.example.portfolio.domain.entity.RebalanceLog;
import com.example.portfolio.service.rebalance.TradeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// ฟีเจอร์ 5: Rebalancing — ดูแผนก่อน (preview) แล้วค่อยซื้อขายจริง (execute) และดูประวัติย้อนหลัง
public interface RebalanceService {

    // method = ชื่อ RebalanceStrategy: threshold (เบี่ยงเกิน 5%), calendar (กลับไปตามเป้าทุกครั้ง)
    List<TradeOrder> preview(Long portfolioId, String method);

    RebalanceLog execute(Long portfolioId, String method);

    Page<RebalanceLog> getHistory(Long portfolioId, Pageable pageable);
}
