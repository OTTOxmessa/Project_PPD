package com.example.portfolio.common;

// Strategy ทุกตัวที่ผู้ใช้เลือกได้จากหน้าเว็บ (เช่น ?method=pivot) ต้องมีชื่อประจำตัว
// StrategyRegistry ใช้ชื่อนี้หา implementation ที่ตรงกัน
// เพิ่ม Strategy ใหม่ = เพิ่มคลาสเดียวที่คืน key() ของตัวเอง ไม่ต้องแก้ if/switch ที่ไหนเลย (OCP)
public interface NamedStrategy {

    String key();
}
