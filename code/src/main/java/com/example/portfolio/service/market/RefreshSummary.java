package com.example.portfolio.service.market;

// สรุปผลการอัปเดตราคาจริงหนึ่งรอบ
public record RefreshSummary(int assetsUpdated, int assetsTotal, int indicesUpdated, int indicesTotal) {
}
