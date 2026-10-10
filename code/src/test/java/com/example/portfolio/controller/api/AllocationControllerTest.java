package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.AllocationTarget;
import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.service.AllocationService;
import com.example.portfolio.service.PortfolioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PUT /allocation/targets/bulk: บันทึกเป้าหมายทั้งตารางในครั้งเดียว และ 400 ถ้ารวมกันเกิน 100%
@ExtendWith(MockitoExtension.class)
class AllocationControllerTest {

    private static final String URL = "/api/v1/portfolios/5/allocation/targets/bulk";

    @Mock
    private AllocationService allocationService;
    @Mock
    private PortfolioService portfolioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new AllocationController(allocationService, portfolioService));
        ControllerTestSupport.loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.logout();
    }

    @Test
    @DisplayName("ส่ง 2 รายการ → 200 พร้อมเป้าหมายที่บันทึกแล้ว และส่งต่อให้ service ครบทั้งชุด")
    void bulkSave() throws Exception {
        when(allocationService.upsertTargets(eq(5L), anyMap())).thenReturn(List.of(
                AllocationTarget.builder().asset(Asset.builder().id(1L).symbol("AAPL").build())
                        .targetPercent(new BigDecimal("40")).build(),
                AllocationTarget.builder().asset(Asset.builder().id(2L).symbol("MSFT").build())
                        .targetPercent(new BigDecimal("60")).build()));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"assetId\":1,\"targetPercent\":40},{\"assetId\":2,\"targetPercent\":60}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].symbol").value("MSFT"));

        verify(allocationService).upsertTargets(5L, Map.of(1L, new BigDecimal("40"), 2L, new BigDecimal("60")));
    }

    @Test
    @DisplayName("รวมกันเกิน 100% → 400 พร้อมข้อความจาก service")
    void totalOver100() throws Exception {
        when(allocationService.upsertTargets(eq(5L), anyMap()))
                .thenThrow(new IllegalArgumentException("สัดส่วนเป้าหมายรวมกันต้องไม่เกิน 100% (ถ้าบันทึกจะรวมเป็น 239%)"));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"assetId\":1,\"targetPercent\":80},{\"assetId\":2,\"targetPercent\":60}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("สัดส่วนเป้าหมายรวมกันต้องไม่เกิน 100% (ถ้าบันทึกจะรวมเป็น 239%)"));
    }

    @Test
    @DisplayName("รายการว่าง หรือมีตัวเกิน 100% → 400 ก่อนถึง service")
    void validation() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"targets\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targets\":[{\"assetId\":1,\"targetPercent\":150}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(allocationService);
    }
}
