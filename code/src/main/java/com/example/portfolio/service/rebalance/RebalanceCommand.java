package com.example.portfolio.service.rebalance;

// Command pattern: ห่อหุ้มการสั่งซื้อ/ขายให้ execute เป็นหน่วยเดียวกัน เรียกผ่าน interface เดียว
public interface RebalanceCommand {

    void execute();
}
