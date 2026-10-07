package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.RebalanceLogResponse;
import com.example.portfolio.dto.response.TradeOrderResponse;
import com.example.portfolio.mapper.RebalanceLogMapper;
import com.example.portfolio.mapper.TradeOrderMapper;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.RebalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ตั้งชื่อแบบ resource (คำนาม) แทนคำกริยา:
//   GET  /portfolios/{id}/rebalance-plan  แผนซื้อขายที่คำนวณได้ตอนนี้ (ยังไม่ทำจริง)
//   POST /portfolios/{id}/rebalances      สร้างการรีบาลานซ์ครั้งใหม่ = ซื้อขายจริง + บันทึก RebalanceLog -> 201
//   GET  /portfolios/{id}/rebalances      ประวัติการรีบาลานซ์ (Pagination & Sorting)
@Tag(name = "Rebalancing", description = "แผนและประวัติการรีบาลานซ์ (Strategy: threshold | calendar)")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}")
@RequiredArgsConstructor
public class RebalanceController {

    private final RebalanceService rebalanceService;
    private final PortfolioService portfolioService;

    @GetMapping("/rebalance-plan")
    @Operation(summary = "ดูแผนรีบาลานซ์ (ยังไม่ซื้อขายจริง)")
    public List<TradeOrderResponse> plan(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long portfolioId,
                                         @RequestParam(defaultValue = "threshold") String method) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return rebalanceService.preview(portfolioId, method).stream()
                .map(TradeOrderMapper::toResponse)
                .toList();
    }

    @PostMapping("/rebalances")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "รีบาลานซ์จริง (201; พอร์ตยังไม่พร้อม เช่น เป้ารวมไม่ถึง 100% = 409; method ไม่รู้จัก = 400)")
    public RebalanceLogResponse execute(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long portfolioId,
                                        @RequestParam(defaultValue = "threshold") String method) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return RebalanceLogMapper.toResponse(rebalanceService.execute(portfolioId, method));
    }

    // ?page=0&size=10&sort=triggeredAt,desc (ค่าเริ่มต้น: ล่าสุดก่อน)
    @GetMapping("/rebalances")
    @Operation(summary = "ประวัติการรีบาลานซ์ (Pagination & Sorting)")
    public Page<RebalanceLogResponse> history(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long portfolioId,
            @ParameterObject @PageableDefault(size = 10, sort = "triggeredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return rebalanceService.getHistory(portfolioId, pageable).map(RebalanceLogMapper::toResponse);
    }
}
