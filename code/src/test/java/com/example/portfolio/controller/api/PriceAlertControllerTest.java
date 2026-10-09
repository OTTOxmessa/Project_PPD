package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceAlert;
import com.example.portfolio.domain.enums.AlertCondition;
import com.example.portfolio.domain.enums.AlertStatus;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.PriceAlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Resource หลักตัวที่ 3: การแจ้งเตือนราคา — CRUD ครบ เป็น sub-resource ของพอร์ต
@ExtendWith(MockitoExtension.class)
class PriceAlertControllerTest {

    private static final String ALERT_JSON = "{\"assetId\":1,\"condition\":\"PRICE_BELOW\",\"targetPrice\":30}";

    @Mock
    private PriceAlertService priceAlertService;
    @Mock
    private PortfolioService portfolioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new PriceAlertController(priceAlertService, portfolioService));
        ControllerTestSupport.loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.logout();
    }

    private static PriceAlert alert(AlertStatus status, String target) {
        return PriceAlert.builder().id(3L).asset(Asset.builder().id(1L).symbol("AAPL").build())
                .condition(AlertCondition.PRICE_BELOW).targetPrice(new BigDecimal(target)).status(status).build();
    }

    @Test
    @DisplayName("GET /portfolios/{id}/alerts → 200 รายการ alert ของพอร์ต")
    void list() throws Exception {
        when(priceAlertService.getByPortfolio(5L)).thenReturn(List.of(alert(AlertStatus.PENDING, "30")));

        mockMvc.perform(get("/api/v1/portfolios/5/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(portfolioService).getByIdForUser(5L, 1L);
    }

    @Test
    @DisplayName("GET /portfolios/{id}/alerts/{alertId} → 200")
    void getOne() throws Exception {
        when(priceAlertService.getById(5L, 3L)).thenReturn(alert(AlertStatus.PENDING, "30"));

        mockMvc.perform(get("/api/v1/portfolios/5/alerts/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("พอร์ตของคนอื่น → 404 และไม่แตะ alert")
    void otherUsersPortfolio() throws Exception {
        when(portfolioService.getByIdForUser(9L, 1L)).thenThrow(new ResourceNotFoundException("Portfolio not found: 9"));

        mockMvc.perform(get("/api/v1/portfolios/9/alerts/3"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(priceAlertService);
    }

    @Test
    @DisplayName("POST /portfolios/{id}/alerts → 201 Created")
    void create() throws Exception {
        when(priceAlertService.create(eq(5L), eq(1L), any(PriceAlert.class))).thenReturn(alert(AlertStatus.PENDING, "30"));

        mockMvc.perform(post("/api/v1/portfolios/5/alerts").contentType(MediaType.APPLICATION_JSON).content(ALERT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("POST ราคาเป้าหมายเป็น 0 → 400")
    void createInvalidTargetPrice() throws Exception {
        mockMvc.perform(post("/api/v1/portfolios/5/alerts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":1,\"condition\":\"PRICE_BELOW\",\"targetPrice\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("targetPrice")));

        verifyNoInteractions(priceAlertService);
    }

    @Test
    @DisplayName("PUT /portfolios/{id}/alerts/{alertId} → 200 พร้อมค่าที่แก้")
    void update() throws Exception {
        when(priceAlertService.update(eq(5L), eq(3L), eq(1L), any(PriceAlert.class)))
                .thenReturn(alert(AlertStatus.PENDING, "28"));

        mockMvc.perform(put("/api/v1/portfolios/5/alerts/3").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":1,\"condition\":\"PRICE_BELOW\",\"targetPrice\":28}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetPrice").value(28));

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(priceAlertService).update(eq(5L), eq(3L), eq(1L), captor.capture());
        assertThat(captor.getValue().getTargetPrice()).isEqualByComparingTo("28");
    }

    @Test
    @DisplayName("PUT alert ที่แจ้งเตือนไปแล้ว → 409")
    void updateNotPending() throws Exception {
        when(priceAlertService.update(eq(5L), eq(3L), eq(1L), any(PriceAlert.class)))
                .thenThrow(new IllegalStateException("แก้ได้เฉพาะ alert ที่ยังรอตรวจ (PENDING)"));

        mockMvc.perform(put("/api/v1/portfolios/5/alerts/3").contentType(MediaType.APPLICATION_JSON).content(ALERT_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /portfolios/{id}/alerts/{alertId} → 204")
    void deleteAlert() throws Exception {
        mockMvc.perform(delete("/api/v1/portfolios/5/alerts/3"))
                .andExpect(status().isNoContent());

        verify(priceAlertService).delete(5L, 3L);
    }
}
