package com.example.portfolio.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// ตั้งเป้าหลายสินทรัพย์ในครั้งเดียว เพื่อให้ตรวจผลรวม 100% กับค่าชุดใหม่ทั้งชุด
// (ถ้าส่งทีละตัว การย้ายสัดส่วนจากตัวหนึ่งไปอีกตัวจะเกิน 100% ชั่วคราวระหว่างทาง)
public record AllocationTargetsRequest(
        @NotEmpty(message = "กรุณาส่งเป้าหมายอย่างน้อย 1 รายการ") List<@Valid AllocationTargetRequest> targets
) {
}
