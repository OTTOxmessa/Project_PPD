package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.HoldingResponse;
import com.example.portfolio.mapper.HoldingMapper;
import com.example.portfolio.service.HoldingService;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

// ไม่มี POST/PUT โดยตั้งใจ — Holding เปลี่ยนแปลงผ่าน TransactionController เท่านั้น
@Tag(name = "Holdings", description = "สินทรัพย์ที่ถืออยู่ในพอร์ตพร้อมมูลค่าตลาด")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;
    private final PortfolioService portfolioService;
    private final QuoteService quoteService;

    @GetMapping
    public List<HoldingResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long portfolioId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return holdingService.getOpenPositions(portfolioId).stream()
                .map(h -> HoldingMapper.toResponse(h,
                        quoteService.latestPrice(h.getAsset().getId()).orElse(null)))
                .toList();
    }
}
