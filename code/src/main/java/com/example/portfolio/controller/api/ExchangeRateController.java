package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.ExchangeRateResponse;
import com.example.portfolio.mapper.ExchangeRateMapper;
import com.example.portfolio.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// อัตราแลกเปลี่ยนสำหรับแสดงผลเป็นเงินบาท: GET /api/v1/exchange-rates/usd-thb
// 200 = มีอัตรา, 503 = ยังดึงจาก Yahoo ไม่ได้ (หน้าเว็บแสดงเป็น USD ต่อไป)
@Tag(name = "Exchange Rates", description = "อัตรา USD/THB สำหรับแสดงผลเป็นเงินบาท (ดูอย่างเดียว)")
@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/usd-thb")
    public ResponseEntity<ExchangeRateResponse> usdToThb() {
        return exchangeRateService.usdToThb()
                .map(ExchangeRateMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }
}
