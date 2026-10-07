package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.PortfolioRequest;
import com.example.portfolio.dto.response.PortfolioResponse;
import com.example.portfolio.dto.response.PortfolioSummaryResponse;
import com.example.portfolio.mapper.PortfolioMapper;
import com.example.portfolio.service.PortfolioService;
import com.example.portfolio.service.PortfolioValuationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Portfolios", description = "พอร์ตการลงทุนของผู้ใช้ (CRUD + Pagination & Sorting)")
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioValuationService portfolioValuationService;

    // Pagination & Sorting: ?page=0&size=10&sort=name,asc
    @GetMapping
    public Page<PortfolioResponse> list(@AuthenticationPrincipal Long userId, @ParameterObject Pageable pageable) {
        return portfolioService.listForUser(userId, pageable).map(PortfolioMapper::toResponse);
    }

    // มูลค่า/กำไรของทุกพอร์ต (คอลัมน์ซ้ายของหน้าหลัก)
    // path "/summary" เป็นค่าตายตัว Spring จึงจับคู่ก่อน "/{id}" เสมอ
    @GetMapping("/summary")
    public List<PortfolioSummaryResponse> summary(@AuthenticationPrincipal Long userId) {
        return portfolioValuationService.summarize(userId).stream()
                .map(PortfolioMapper::toSummaryResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public PortfolioResponse get(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return PortfolioMapper.toResponse(portfolioService.getByIdForUser(id, userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioResponse create(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody PortfolioRequest request) {
        return PortfolioMapper.toResponse(portfolioService.create(userId, PortfolioMapper.toEntity(request)));
    }

    @PutMapping("/{id}")
    public PortfolioResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                    @Valid @RequestBody PortfolioRequest request) {
        return PortfolioMapper.toResponse(portfolioService.update(id, userId, PortfolioMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        portfolioService.delete(id, userId);
    }
}
