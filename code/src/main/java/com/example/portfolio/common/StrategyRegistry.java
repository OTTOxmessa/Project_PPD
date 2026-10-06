package com.example.portfolio.common;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// รวม Strategy ทุกตัวของงานเดียวกัน (Spring ฉีดมาเป็น List) แล้วหาตัวที่ต้องการจากชื่อ
// ใช้ร่วมกันใน Allocation / Support-Resistance / Rebalance แทนการเขียน switch ซ้ำในแต่ละที่
public final class StrategyRegistry<T extends NamedStrategy> {

    private final String label;
    private final Map<String, T> byKey;

    public StrategyRegistry(String label, Collection<? extends T> strategies) {
        this.label = label;
        Map<String, T> map = new LinkedHashMap<>();
        for (T strategy : strategies) {
            T previous = map.putIfAbsent(normalize(strategy.key()), strategy);
            if (previous != null) {
                throw new IllegalStateException("Strategy สำหรับ" + label + " ใช้ชื่อซ้ำกัน: " + strategy.key());
            }
        }
        this.byKey = Collections.unmodifiableMap(map);
    }

    // ชื่อที่ไม่รู้จัก -> IllegalArgumentException (GlobalExceptionHandler ตอบ 400 พร้อมบอกชื่อที่ใช้ได้)
    public T get(String key) {
        T strategy = key == null ? null : byKey.get(normalize(key));
        if (strategy == null) {
            throw new IllegalArgumentException("ไม่รู้จักวิธี" + label + " '" + key + "' (ใช้ได้: "
                    + String.join(", ", keys()) + ")");
        }
        return strategy;
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
