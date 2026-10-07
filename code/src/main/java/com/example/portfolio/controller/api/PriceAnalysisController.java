package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.SupportResistanceResponse;
import com.example.portfolio.mapper.SupportResistanceMapper;
import com.example.portfolio.service.SupportResistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Price Analysis", description = "แนวรับ-แนวต้าน (Strategy: pivot | ma)")
@RestController
@RequestMapping("/api/v1/assets/{assetId}/support-resistance")
@RequiredArgsConstructor
public class PriceAnalysisController {

    private final SupportResistanceService supportResistanceService;

    // ?method=pivot|ma&from=&to= — default เป็น pivot
    @GetMapping
    public SupportResistanceResponse calculate(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "pivot") String method,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return SupportResistanceMapper.toResponse(supportResistanceService.calculate(assetId, from, to, method));
    }
}
