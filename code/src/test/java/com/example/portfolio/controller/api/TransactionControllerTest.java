package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.TransactionService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ทดสอบ API บันทึกการซื้อขาย: 201 / 400 / 404 / 409 และการตรวจสิทธิ์เจ้าของพอร์ตก่อนทำรายการ
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final String URL = "/api/v1/portfolios/5/transactions";

    @Mock
    private TransactionService transactionService;
    @Mock
    private PortfolioService portfolioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(new TransactionController(transactionService, portfolioService));
        ControllerTestSupport.loginAs(1L);
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.logout();
    }

    private static Transaction saved(TransactionType type) {
        return Transaction.builder()
                .id(100L)
                .asset(Asset.builder().id(10L).symbol("AAPL").build())
                .type(type)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("35.5"))
                .executedAt(LocalDateTime.of(2026, 10, 6, 12, 0))
                .build();
    }

    @Test
    @DisplayName("POST BUY ที่ถูกต้อง → 201 และส่งค่าให้ service ครบ")
    void recordBuy() throws Exception {
        when(transactionService.record(eq(5L), eq(10L), eq(TransactionType.BUY),
                eq(new BigDecimal("100")), eq(new BigDecimal("35.5")), isNull()))
                .thenReturn(saved(TransactionType.BUY));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\",\"quantity\":100,\"price\":35.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.type").value("BUY"));

        verify(portfolioService).getByIdForUser(5L, 1L);
    }

    @Test
    @DisplayName("จำนวนเป็น 0 → 400 และไม่บันทึก")
    void zeroQuantityRejected() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\",\"quantity\":0,\"price\":35.5}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("ราคาเป็น 0 → 400 และ details บอกชื่อฟิลด์ price พร้อมข้อความภาษาไทย")
    void zeroPriceRejectedWithFieldName() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\",\"quantity\":1,\"price\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ผ่านการตรวจสอบ"))
                .andExpect(jsonPath("$.details[0]").value("price: ราคาต่อหน่วยต้องมากกว่า 0"));

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("ไม่กรอกทั้งจำนวนและราคา → 400 และ details บอกครบทั้ง 2 ฟิลด์")
    void missingFieldsAllReported() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)))
                .andExpect(jsonPath("$.details", hasItem("quantity: กรุณากรอกจำนวน")))
                .andExpect(jsonPath("$.details", hasItem("price: กรุณากรอกราคาต่อหน่วย")));
    }

    @Test
    @DisplayName("วันที่ทำรายการเป็นอนาคต → 400")
    void futureDateRejected() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\",\"quantity\":1,\"price\":1,\"executedAt\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("ขายเกินจำนวนที่ถือ → 409 Conflict")
    void oversellConflict() throws Exception {
        when(transactionService.record(anyLong(), anyLong(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("ขายเกินจำนวนที่ถืออยู่ (มี 10)"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"SELL\",\"quantity\":50,\"price\":35}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("พอร์ตไม่ใช่ของผู้ใช้ → 404 และไม่บันทึกรายการ")
    void foreignPortfolio() throws Exception {
        when(portfolioService.getByIdForUser(5L, 1L)).thenThrow(new ResourceNotFoundException("Portfolio not found: 5"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":10,\"type\":\"BUY\",\"quantity\":1,\"price\":1}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("GET รายการธุรกรรมของพอร์ต → 200")
    void list() throws Exception {
        when(transactionService.getByPortfolio(5L)).thenReturn(List.of(saved(TransactionType.BUY), saved(TransactionType.SELL)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].type").value("SELL"));
    }

    @Test
    @DisplayName("PUT /transactions/{id} → 200 พร้อมรายการที่แก้แล้ว (วันที่แปลงเป็นเที่ยงวัน)")
    void update() throws Exception {
        when(transactionService.update(eq(5L), eq(100L), eq(TransactionType.SELL), any(), any(), any()))
                .thenReturn(saved(TransactionType.SELL));

        mockMvc.perform(put(URL + "/100").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SELL\",\"quantity\":100,\"price\":35.5,\"executedAt\":\"2026-10-06\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.type").value("SELL"));

        verify(transactionService).update(5L, 100L, TransactionType.SELL, new BigDecimal("100"), new BigDecimal("35.5"),
                LocalDateTime.of(2026, 10, 6, 12, 0));
    }

    @Test
    @DisplayName("PUT จำนวน 0 → 400 และไม่แก้")
    void updateValidation() throws Exception {
        mockMvc.perform(put(URL + "/100").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUY\",\"quantity\":0,\"price\":10}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("PUT แก้แล้วขายเกินจำนวนที่ถือ → 409")
    void updateConflict() throws Exception {
        when(transactionService.update(anyLong(), anyLong(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("แก้ไขไม่ได้: รายการวันที่ 2026-10-06 จะกลายเป็นขายเกินจำนวนที่ถืออยู่ ณ ตอนนั้น"));

        mockMvc.perform(put(URL + "/100").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUY\",\"quantity\":1,\"price\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /transactions/{id} → 204")
    void deleteTransaction() throws Exception {
        mockMvc.perform(delete(URL + "/100"))
                .andExpect(status().isNoContent());

        verify(transactionService).delete(5L, 100L);
    }

    @Test
    @DisplayName("DELETE รายการที่ไม่มีในพอร์ตนี้ → 404")
    void deleteNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Transaction not found: 999"))
                .when(transactionService).delete(5L, 999L);

        mockMvc.perform(delete(URL + "/999"))
                .andExpect(status().isNotFound());
    }
}
