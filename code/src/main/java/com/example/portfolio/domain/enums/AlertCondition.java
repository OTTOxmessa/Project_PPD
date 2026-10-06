package com.example.portfolio.domain.enums;

// เงื่อนไขที่ระบบรองรับจริงเท่านั้น — แต่ละค่ามี AlertConditionEvaluator ของตัวเองใน service/alert/condition
// เพิ่มเงื่อนไขใหม่ = เพิ่มค่าที่นี่ + เพิ่ม evaluator อีกหนึ่งคลาส
public enum AlertCondition {
    PRICE_ABOVE,
    PRICE_BELOW
}
