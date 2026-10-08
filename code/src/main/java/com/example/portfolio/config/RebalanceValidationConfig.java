package com.example.portfolio.config;

import com.example.portfolio.service.rebalance.AllocationTargetsDefinedHandler;
import com.example.portfolio.service.rebalance.MinimumHoldingsValidationHandler;
import com.example.portfolio.service.rebalance.RebalanceValidationHandler;
import com.example.portfolio.service.rebalance.TargetSumValidationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ประกอบ Chain of Responsibility ของการตรวจก่อนรีบาลานซ์ไว้ที่เดียว
// ลำดับสำคัญ: ต้องมี holding -> ต้องมีเป้าหมาย -> เป้าหมายต้องรวม 100%
// เพิ่มเงื่อนไขใหม่ = สร้าง handler ใหม่ แล้วต่อท้ายตรงนี้ (RebalanceService ไม่ต้องแก้)
@Configuration
public class RebalanceValidationConfig {

    @Bean
    public RebalanceValidationHandler rebalanceValidationChain() {
        RebalanceValidationHandler first = new MinimumHoldingsValidationHandler();
        first.setNext(new AllocationTargetsDefinedHandler())
                .setNext(new TargetSumValidationHandler());
        return first;
    }
}
