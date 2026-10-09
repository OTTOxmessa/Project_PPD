package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.PortfolioSummary;
import com.example.portfolio.service.PortfolioValuationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

// ทดสอบ REST API ของพอร์ต: CRUD, status code, validation, pagination และ error format
@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;
    @Mock
    private PortfolioValuationService portfolioValuationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new PortfolioController(portfolioService, portfolioValuationService));
        ControllerTestSupport.loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.logout();
    }

    private static Portfolio portfolio(Long id, String name) {
        return Portfolio.builder().id(id).name(name).baseCurrency("USD").build();
    }

    @Test
    @DisplayName("GET /portfolios/{id} → 200 พร้อมข้อมูลพอร์ต")
    void getById() throws Exception {
        when(portfolioService.getByIdForUser(5L, 1L)).thenReturn(portfolio(5L, "Long-term"));

        mockMvc.perform(get("/api/v1/portfolios/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Long-term"))
                .andExpect(jsonPath("$.baseCurrency").value("USD"));
    }

    @Test
    @DisplayName("GET /portfolios/{id} ของคนอื่น → 404 ในรูปแบบ ErrorResponse")
    void getByIdNotFound() throws Exception {
        when(portfolioService.getByIdForUser(5L, 1L)).thenThrow(new ResourceNotFoundException("Portfolio not found: 5"));

        mockMvc.perform(get("/api/v1/portfolios/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Portfolio not found: 5"));
    }

    @Test
    @DisplayName("GET /portfolios?page=1&size=5&sort=name,asc → ส่ง Pageable ที่ถูกต้องให้ service")
    void listWithPaginationAndSorting() throws Exception {
        PageRequest pageRequest = PageRequest.of(1, 5, Sort.by("name"));
        when(portfolioService.listForUser(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(portfolio(6L, "Dividend")), pageRequest, 6));

        mockMvc.perform(get("/api/v1/portfolios").param("page", "1").param("size", "5").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Dividend"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(portfolioService).listForUser(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull()
                .satisfies(order -> assertThat(order.isAscending()).isTrue());
    }

    @Test
    @DisplayName("GET /portfolios/summary → 200 สรุปมูลค่าทุกพอร์ต")
    void summary() throws Exception {
        when(portfolioValuationService.summarize(1L)).thenReturn(List.of(new PortfolioSummary(
                5L, "Long-term", "USD", new BigDecimal("12000"), new BigDecimal("10000"),
                new BigDecimal("2000"), new BigDecimal("20"), 3)));

        mockMvc.perform(get("/api/v1/portfolios/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Long-term"))
                .andExpect(jsonPath("$[0].holdingsCount").value(3));
    }

    @Test
    @DisplayName("POST /portfolios → 201 และสร้างให้ผู้ใช้ที่ login อยู่")
    void create() throws Exception {
        when(portfolioService.create(eq(1L), any(Portfolio.class))).thenReturn(portfolio(7L, "US Tech"));

        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"US Tech\",\"baseCurrency\":\"USD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioService).create(eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("US Tech");
        assertThat(captor.getValue().getBaseCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("POST /portfolios ชื่อว่าง → 400 พร้อมรายละเอียดฟิลด์ที่ผิด และไม่เรียก service")
    void createValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"baseCurrency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(containsString("name")));

        verifyNoInteractions(portfolioService);
    }

    @Test
    @DisplayName("POST /portfolios สกุลเงิน THB → 400 เพราะระบบรองรับเฉพาะ USD และไม่เรียก service")
    void createRejectsNonUsdCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thai Stocks\",\"baseCurrency\":\"THB\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("baseCurrency")))
                .andExpect(jsonPath("$.details[0]").value(containsString("USD")));

        verifyNoInteractions(portfolioService);
    }

    @Test
    @DisplayName("PUT /portfolios/{id} → 200 พร้อมค่าที่แก้แล้ว")
    void update() throws Exception {
        when(portfolioService.update(eq(5L), eq(1L), any(Portfolio.class))).thenReturn(portfolio(5L, "Renamed"));

        mockMvc.perform(put("/api/v1/portfolios/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"baseCurrency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    @DisplayName("DELETE /portfolios/{id} → 204 No Content")
    void deletePortfolio() throws Exception {
        mockMvc.perform(delete("/api/v1/portfolios/5"))
                .andExpect(status().isNoContent());

        verify(portfolioService).delete(5L, 1L);
    }

    @Test
    @DisplayName("ข้อผิดพลาดที่ไม่คาดคิด → 500 โดยไม่เปิดเผยรายละเอียดภายใน")
    void unexpectedError() throws Exception {
        when(portfolioService.getByIdForUser(5L, 1L)).thenThrow(new RuntimeException("database password is root"));

        mockMvc.perform(get("/api/v1/portfolios/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("เกิดข้อผิดพลาดที่ไม่คาดคิด"));
    }

    @Test
    @DisplayName("JSON ผิดรูปแบบ → 400 (ไม่ใช่ 500)")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(portfolioService);
    }

    @Test
    @DisplayName("id ไม่ใช่ตัวเลข (/portfolios/abc) → 400")
    void idTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("id")));
    }
}
