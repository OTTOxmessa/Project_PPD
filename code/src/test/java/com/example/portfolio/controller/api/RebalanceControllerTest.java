package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.RebalanceLog;
import com.example.portfolio.domain.enums.RebalanceMethod;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.RebalanceService;
import com.example.portfolio.service.rebalance.TradeOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// endpoint แบบ resource: /rebalance-plan (ดูแผน), /rebalances (สร้าง = 201, ดูประวัติ = pagination)
@ExtendWith(MockitoExtension.class)
class RebalanceControllerTest {

    @Mock
    private RebalanceService rebalanceService;
    @Mock
    private PortfolioService portfolioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new RebalanceController(rebalanceService, portfolioService));
        ControllerTestSupport.loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.logout();
    }

    private static RebalanceLog log() {
        return RebalanceLog.builder().id(4L).method(RebalanceMethod.CALENDAR)
                .triggeredAt(LocalDateTime.of(2026, 10, 1, 9, 0)).details("[]").build();
    }

    @Test
    @DisplayName("GET /portfolios/{id}/rebalance-plan?method=calendar → 200 รายการคำสั่งซื้อขาย")
    void plan() throws Exception {
        when(rebalanceService.preview(5L, "calendar")).thenReturn(List.of(
                new TradeOrder(1L, "PTT", TransactionType.SELL, new BigDecimal("10"), new BigDecimal("34"))));

        mockMvc.perform(get("/api/v1/portfolios/5/rebalance-plan").param("method", "calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SELL"));
    }

    @Test
    @DisplayName("POST /portfolios/{id}/rebalances → 201 Created พร้อม RebalanceLog")
    void execute() throws Exception {
        when(rebalanceService.execute(5L, "calendar")).thenReturn(log());

        mockMvc.perform(post("/api/v1/portfolios/5/rebalances").param("method", "calendar"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.method").value("CALENDAR"));
    }

    @Test
    @DisplayName("POST ชื่อวิธีที่ไม่รู้จัก → 400")
    void unknownMethod() throws Exception {
        when(rebalanceService.execute(5L, "magic")).thenThrow(new IllegalArgumentException("ไม่รู้จักวิธีรีบาลานซ์ 'magic'"));

        mockMvc.perform(post("/api/v1/portfolios/5/rebalances").param("method", "magic"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /portfolios/{id}/rebalances → ประวัติแบบแบ่งหน้า ค่าเริ่มต้นเรียงล่าสุดก่อน")
    void historyDefaultsToNewestFirst() throws Exception {
        when(rebalanceService.getHistory(eq(5L), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(log()), inv.getArgument(1), 1));

        mockMvc.perform(get("/api/v1/portfolios/5/rebalances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(4));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(rebalanceService).getHistory(eq(5L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("triggeredAt"))
                .isNotNull()
                .satisfies(order -> assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC));
    }
}
