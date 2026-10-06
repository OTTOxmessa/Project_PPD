package com.example.portfolio.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ทดสอบตัวเลือก Strategy จากชื่อ ที่ใช้แทน switch/if ใน controller
class StrategyRegistryTest {

    private record Fake(String key) implements NamedStrategy {
    }

    private final StrategyRegistry<Fake> registry =
            new StrategyRegistry<>("ทดสอบ", List.of(new Fake("pivot"), new Fake("ma")));

    @Test
    @DisplayName("หา Strategy จากชื่อได้ ไม่สนตัวพิมพ์เล็ก-ใหญ่และช่องว่าง")
    void findsByKey() {
        assertThat(registry.get("pivot").key()).isEqualTo("pivot");
        assertThat(registry.get(" MA ").key()).isEqualTo("ma");
    }

    @Test
    @DisplayName("ชื่อที่ไม่รู้จัก → IllegalArgumentException พร้อมบอกชื่อที่ใช้ได้")
    void unknownKey() {
        assertThatThrownBy(() -> registry.get("fibonacci"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pivot")
                .hasMessageContaining("ma");
        assertThatThrownBy(() -> registry.get(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Strategy สองตัวใช้ชื่อซ้ำกัน → แจ้งตั้งแต่ตอนสร้าง (แอปไม่ start)")
    void duplicateKeysRejected() {
        assertThatThrownBy(() -> new StrategyRegistry<>("ทดสอบ", List.of(new Fake("ma"), new Fake("MA"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("keys() คืนชื่อตามลำดับที่ลงทะเบียน")
    void listsKeys() {
        assertThat(registry.keys()).containsExactly("pivot", "ma");
    }
}
