package com.example.portfolio.controller.api;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.service.AssetMaintenanceService;
import com.example.portfolio.service.AssetService;
import com.example.portfolio.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Resource หลักตัวที่ 2: สินทรัพย์ — CRUD ครบ 4 method และ status code 200 / 201 / 204 / 400 / 404 / 409
@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

    private static final String AAPL_JSON =
            "{\"symbol\":\"AAPL\",\"name\":\"Apple Inc.\",\"assetType\":\"STOCK\",\"exchange\":\"US\"}";

    @Mock
    private AssetService assetService;
    @Mock
    private AssetMaintenanceService assetMaintenanceService;
    @Mock
    private QuoteService quoteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.mockMvcFor(
                new AssetController(assetService, assetMaintenanceService, quoteService));
    }

    private static Asset aapl(String name) {
        return Asset.builder().id(1L).symbol("AAPL").name(name).assetType(AssetType.STOCK).exchange("US").build();
    }

    @Test
    @DisplayName("GET /assets → 200 รายการสินทรัพย์")
    void list() throws Exception {
        when(assetService.getAll()).thenReturn(List.of(aapl("Apple Inc.")));

        mockMvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    @DisplayName("GET /assets/{id} ไม่มีอยู่ → 404 พร้อม path ใน ErrorResponse")
    void getNotFound() throws Exception {
        when(assetService.getById(99L)).thenThrow(new ResourceNotFoundException("Asset not found: 99"));

        mockMvc.perform(get("/api/v1/assets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/assets/99"));
    }

    @Test
    @DisplayName("POST /assets → 201 Created")
    void create() throws Exception {
        when(assetService.create(any(Asset.class))).thenReturn(aapl("Apple Inc."));

        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content(AAPL_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /assets ไม่ระบุประเภท → 400 และไม่เรียก service")
    void createValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\",\"name\":\"Apple Inc.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("assetType")));

        verifyNoInteractions(assetService);
    }

    @Test
    @DisplayName("POST /assets symbol ซ้ำ → 409 Conflict")
    void createDuplicate() throws Exception {
        when(assetService.create(any(Asset.class))).thenThrow(new IllegalStateException("มีสินทรัพย์ symbol AAPL อยู่แล้ว"));

        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content(AAPL_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /assets/{id} → 200 พร้อมค่าที่แก้แล้ว")
    void update() throws Exception {
        when(assetMaintenanceService.update(eq(1L), any(Asset.class))).thenReturn(aapl("Apple Incorporated"));

        mockMvc.perform(put("/api/v1/assets/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\",\"name\":\"Apple Incorporated\",\"assetType\":\"STOCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Apple Incorporated"));

        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetMaintenanceService).update(eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Apple Incorporated");
    }

    @Test
    @DisplayName("PUT /assets/{id} เปลี่ยน symbol → 409")
    void updateSymbolConflict() throws Exception {
        when(assetMaintenanceService.update(eq(1L), any(Asset.class)))
                .thenThrow(new IllegalStateException("เปลี่ยน symbol ไม่ได้"));

        mockMvc.perform(put("/api/v1/assets/1").contentType(MediaType.APPLICATION_JSON).content(AAPL_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /assets/{id} → 204 No Content")
    void deleteAsset() throws Exception {
        mockMvc.perform(delete("/api/v1/assets/1"))
                .andExpect(status().isNoContent());

        verify(assetMaintenanceService).delete(1L);
    }

    @Test
    @DisplayName("DELETE /assets/{id} ที่ยังมีพอร์ตถือ → 409")
    void deleteInUse() throws Exception {
        doThrow(new IllegalStateException("ลบ AAPL ไม่ได้")).when(assetMaintenanceService).delete(1L);

        mockMvc.perform(delete("/api/v1/assets/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("ลบ AAPL ไม่ได้"));
    }

    @Test
    @DisplayName("ฐานข้อมูลปฏิเสธเพราะ constraint → 409 โดยไม่เปิดเผยข้อความจากฐานข้อมูล")
    void dataIntegrityViolation() throws Exception {
        doThrow(new DataIntegrityViolationException("violates foreign key constraint fk_holdings_asset_id"))
                .when(assetMaintenanceService).delete(1L);

        mockMvc.perform(delete("/api/v1/assets/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fk_holdings"))));
    }

    @Test
    @DisplayName("PUT /assets/by-symbol/{symbol} → 200 หา/สร้างจาก symbol ใน URL (idempotent)")
    void ensureBySymbol() throws Exception {
        when(assetService.ensure(eq("AAPL"), isNull(), isNull(), isNull(), eq(new BigDecimal("34.5"))))
                .thenReturn(aapl("Apple Inc."));

        mockMvc.perform(put("/api/v1/assets/by-symbol/AAPL").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referencePrice\":34.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
