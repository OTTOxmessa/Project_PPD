package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.AllocationTargetRequest;
import com.example.portfolio.dto.request.AllocationTargetsRequest;
import com.example.portfolio.dto.response.AllocationComparisonResponse;
import com.example.portfolio.dto.response.AllocationTargetResponse;
import com.example.portfolio.mapper.AllocationComparisonMapper;
import com.example.portfolio.mapper.AllocationTargetMapper;
import com.example.portfolio.service.AllocationService;
import com.example.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Allocation", description = "สัดส่วนปัจจุบันเทียบเป้าหมาย (Strategy: target | equal | risk) และกำหนดเป้าหมาย")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/allocation")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;
    private final PortfolioService portfolioService;

    // ?method=target|equal|risk — ส่งชื่อต่อให้ service เลือก Strategy เอง (controller ไม่รู้จักคลาส Strategy)
    @GetMapping
    public List<AllocationComparisonResponse> compare(@AuthenticationPrincipal Long userId,
                                                      @PathVariable Long portfolioId,
                                                      @RequestParam(defaultValue = "target") String method) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return allocationService.compare(portfolioId, method).stream()
                .map(AllocationComparisonMapper::toResponse)
                .toList();
    }

    @GetMapping("/targets")
    public List<AllocationTargetResponse> getTargets(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long portfolioId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return allocationService.getTargets(portfolioId).stream()
                .map(AllocationTargetMapper::toResponse)
                .toList();
    }

    @PutMapping("/targets")
    public AllocationTargetResponse setTarget(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long portfolioId,
                                              @Valid @RequestBody AllocationTargetRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return AllocationTargetMapper.toResponse(
                allocationService.upsertTarget(portfolioId, request.assetId(), request.targetPercent()));
    }

    // หน้าเว็บบันทึกเป้าหมายทั้งตารางผ่าน endpoint นี้ — 400 ถ้ารวมกันเกิน 100%
    @PutMapping("/targets/bulk")
    public List<AllocationTargetResponse> setTargets(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long portfolioId,
                                                     @Valid @RequestBody AllocationTargetsRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        Map<Long, BigDecimal> targets = new LinkedHashMap<>();
        request.targets().forEach(t -> targets.put(t.assetId(), t.targetPercent()));
        return allocationService.upsertTargets(portfolioId, targets).stream()
                .map(AllocationTargetMapper::toResponse)
                .toList();
    }
}
