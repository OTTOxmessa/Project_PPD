package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.TransactionRequest;
import com.example.portfolio.dto.request.TransactionUpdateRequest;
import com.example.portfolio.dto.response.TransactionResponse;
import com.example.portfolio.mapper.TransactionMapper;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transactions", description = "ประวัติ บันทึก แก้ไข และลบรายการซื้อ-ขาย")
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PortfolioService portfolioService;

    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal Long userId, @PathVariable Long portfolioId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        return transactionService.getByPortfolio(portfolioId).stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse record(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long portfolioId,
                                       @Valid @RequestBody TransactionRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        // ระบุวันที่ย้อนหลังได้ (เช่น บันทึกการซื้อที่เกิดขึ้นจริงเมื่อเดือนก่อน) ไม่ระบุ = ตอนนี้
        LocalDateTime executedAt = request.executedAt() != null ? request.executedAt().atTime(12, 0) : null;
        var transaction = transactionService.record(portfolioId, request.assetId(), request.type(),
                request.quantity(), request.price(), executedAt);
        return TransactionMapper.toResponse(transaction);
    }

    // แก้รายการย้อนหลัง แล้วระบบคำนวณจำนวนหุ้นและต้นทุนเฉลี่ยใหม่จากประวัติทั้งหมด
    // 409 ถ้าแก้แล้วทำให้มีจุดที่ขายเกินจำนวนที่ถืออยู่ ณ ตอนนั้น
    @PutMapping("/{transactionId}")
    public TransactionResponse update(@AuthenticationPrincipal Long userId,
                                      @PathVariable Long portfolioId,
                                      @PathVariable Long transactionId,
                                      @Valid @RequestBody TransactionUpdateRequest request) {
        portfolioService.getByIdForUser(portfolioId, userId);
        LocalDateTime executedAt = request.executedAt() != null ? request.executedAt().atTime(12, 0) : null;
        var transaction = transactionService.update(portfolioId, transactionId, request.type(),
                request.quantity(), request.price(), executedAt);
        return TransactionMapper.toResponse(transaction);
    }

    // 409 ถ้าลบแล้วรายการขายที่ตามมาจะขายเกินจำนวนที่ถือ (เช่น ลบรายการซื้อที่ขายไปแล้ว)
    @DeleteMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId,
                       @PathVariable Long portfolioId,
                       @PathVariable Long transactionId) {
        portfolioService.getByIdForUser(portfolioId, userId);
        transactionService.delete(portfolioId, transactionId);
    }
}
