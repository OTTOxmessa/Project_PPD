package com.example.portfolio.controller.api;

import com.example.portfolio.service.ExchangeRateService;
import com.example.portfolio.service.market.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /api/v1/exchange-rates/usd-thb: 200 พร้อมอัตรา หรือ 503 เมื่อยังไม่มีอัตรา
@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new ExchangeRateController(exchangeRateService));
    }

    @Test
    @DisplayName("มีอัตรา → 200 {base: USD, quote: THB, rate, asOf}")
    void returnsRate() throws Exception {
        when(exchangeRateService.usdToThb()).thenReturn(Optional.of(
                new ExchangeRate("USD", "THB", new BigDecimal("32.85"), LocalDate.of(2026, 10, 8))));

        mockMvc.perform(get("/api/v1/exchange-rates/usd-thb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.quote").value("THB"))
                .andExpect(jsonPath("$.rate").value(32.85))
                .andExpect(jsonPath("$.asOf").exists());
    }

    @Test
    @DisplayName("ยังดึงอัตราไม่ได้ → 503")
    void unavailable() throws Exception {
        when(exchangeRateService.usdToThb()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/exchange-rates/usd-thb"))
                .andExpect(status().isServiceUnavailable());
    }
}
