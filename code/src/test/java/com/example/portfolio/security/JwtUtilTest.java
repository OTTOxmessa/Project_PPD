package com.example.portfolio.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ทดสอบการออกและตรวจ JWT โดยไม่ต้องเปิด Spring context (ค่าตั้งค่าส่งผ่าน constructor)
class JwtUtilTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-at-least-32-bytes";

    @Test
    @DisplayName("token ที่ออกให้ ถอดกลับได้ userId เดิม")
    void roundTrip() {
        TokenProvider provider = new JwtUtil(SECRET, 60_000);

        String token = provider.generateToken(42L, "otto@example.com");

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("token ที่เซ็นด้วย secret อื่น → ไม่ผ่าน")
    void wrongSecret() {
        String foreignToken = new JwtUtil("another-secret-that-is-also-long-enough-32b", 60_000)
                .generateToken(42L, "otto@example.com");

        assertThat(new JwtUtil(SECRET, 60_000).isValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("token หมดอายุ → ไม่ผ่าน")
    void expiredToken() {
        TokenProvider provider = new JwtUtil(SECRET, -1_000);

        assertThat(provider.isValid(provider.generateToken(42L, "otto@example.com"))).isFalse();
    }

    @Test
    @DisplayName("ข้อความที่ไม่ใช่ JWT → ไม่ผ่าน และไม่ throw ออกมา")
    void malformedToken() {
        assertThat(new JwtUtil(SECRET, 60_000).isValid("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("secret สั้นเกินไป → แอปไม่ยอม start (แจ้งตั้งแต่สร้าง ไม่ใช่ตอนมีคน login)")
    void secretTooShort() {
        assertThatThrownBy(() -> new JwtUtil("short", 60_000)).isInstanceOf(RuntimeException.class);
    }
}
