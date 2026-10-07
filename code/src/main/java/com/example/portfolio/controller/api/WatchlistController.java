package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.WatchlistRequest;
import com.example.portfolio.dto.response.QuoteResponse;
import com.example.portfolio.mapper.QuoteMapper;
import com.example.portfolio.service.QuoteService;
import com.example.portfolio.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Watchlist", description = "หุ้นที่ผู้ใช้ติดตาม")
@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final QuoteService quoteService;

    // คืนราคาล่าสุด + % เปลี่ยนแปลงรายวันของทุกตัวใน watchlist
    @GetMapping
    public List<QuoteResponse> list(@AuthenticationPrincipal Long userId) {
        return watchlistService.getWatchlist(userId).stream()
                .map(quoteService::getQuote)
                .map(QuoteMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse add(@AuthenticationPrincipal Long userId, @Valid @RequestBody WatchlistRequest request) {
        return QuoteMapper.toResponse(quoteService.getQuote(watchlistService.add(userId, request.assetId())));
    }

    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Long userId, @PathVariable Long assetId) {
        watchlistService.remove(userId, assetId);
    }
}
