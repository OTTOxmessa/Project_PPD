package com.example.portfolio.service.rebalance;

import java.util.List;
import java.util.stream.Collectors;

// แปลงรายการคำสั่งเป็นข้อความ JSON สำหรับเก็บในคอลัมน์ rebalance_logs.details
// แยกออกจาก RebalanceService เพื่อให้ service ทำหน้าที่ประสานงานอย่างเดียว (SRP)
public final class TradeOrderFormatter {

    private TradeOrderFormatter() {
    }

    public static String toJson(List<TradeOrder> orders) {
        return orders.stream()
                .map(o -> String.format("{\"symbol\":\"%s\",\"type\":\"%s\",\"quantity\":%s,\"price\":%s}",
                        o.symbol(), o.type(), o.quantity().toPlainString(), o.estimatedPrice().toPlainString()))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
