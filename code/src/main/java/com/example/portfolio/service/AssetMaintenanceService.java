package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Asset;

// แก้ไข / ลบสินทรัพย์ (master data) — แยกจาก AssetService ที่ใช้ดูและเพิ่มสินทรัพย์ (ISP)
// หน้าที่ส่วนใหญ่ของระบบแค่อ่านสินทรัพย์ จึงไม่ควรต้องเห็นเมธอดแก้/ลบ
public interface AssetMaintenanceService {

    // แก้ชื่อ ประเภท และตลาดได้ แต่เปลี่ยน symbol ไม่ได้ เพราะราคาย้อนหลังผูกกับ symbol เดิม
    Asset update(Long id, Asset changes);

    // ลบได้เฉพาะสินทรัพย์ที่ไม่มีพอร์ตไหนถือ ไม่มีธุรกรรม alert หรือเป้าหมายอ้างถึง
    void delete(Long id);
}
