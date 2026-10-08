package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.PriceAlertRequest;
import com.example.portfolio.dto.response.PriceAlertResponse;
import com.example.portfolio.mapper.PriceAlertMapper;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.PriceAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Resource หลัก: การแจ้งเตือนราคา (CRUD ครบ) เป็น sub-resource ของพอร์ต
// ทุก endpoint ตรวจก่อนว่าพอร์ตเป็นของผู้ใช้ที่ login อยู่ (พอร์ตคนอื่น = 404)
@Tag(name = "Price Alerts", description = "แจ้งเตือนเมื่อราคาถึงเป้า (CRUD)")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;
    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "รายการ alert ของพอร์ต")
    public List<PriceAlertResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long portfolioId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return priceAlertService.getByPortfolio(portfolioId).stream()
                .map(PriceAlertMapper::toResponse)
                .toList();
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "ดู alert")
    public PriceAlertResponse get(@AuthenticationPrincipal Long userId,
                                  @PathVariable Long portfolioId,
                                  @PathVariable Long alertId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return PriceAlertMapper.toResponse(priceAlertService.getById(portfolioId, alertId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "สร้าง alert (สถานะเริ่มต้น PENDING)")
    public PriceAlertResponse create(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long portfolioId,
                                     @Valid @RequestBody PriceAlertRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return PriceAlertMapper.toResponse(
                priceAlertService.create(portfolioId, request.assetId(), PriceAlertMapper.toEntity(request)));
    }

    @PutMapping("/{alertId}")
    @Operation(summary = "แก้ alert (เฉพาะสถานะ PENDING ไม่เช่นนั้น 409)")
    public PriceAlertResponse update(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long portfolioId,
                                     @PathVariable Long alertId,
                                     @Valid @RequestBody PriceAlertRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return PriceAlertMapper.toResponse(priceAlertService.update(
                portfolioId, alertId, request.assetId(), PriceAlertMapper.toEntity(request)));
    }

    @DeleteMapping("/{alertId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "ลบ alert")
    public void delete(@AuthenticationPrincipal Long userId,
                       @PathVariable Long portfolioId,
                       @PathVariable Long alertId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        // service เช็คต่ออีกชั้นว่า alert นี้อยู่ในพอร์ตนี้จริง (กันการลบ alert ของพอร์ตอื่นด้วยการเดา alertId)
        priceAlertService.delete(portfolioId, alertId);
    }
}
