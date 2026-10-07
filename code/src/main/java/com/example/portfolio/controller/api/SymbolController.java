package com.example.portfolio.controller.api;

import com.example.portfolio.dto.response.SymbolSuggestionResponse;
import com.example.portfolio.mapper.QuoteMapper;
import com.example.portfolio.service.SymbolSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

// Auto-complete: GET /api/v1/symbols/search?q=aa -> AA, AAL, AAP, AAOI, AAPL ...
@Tag(name = "Symbols", description = "ค้นหาหุ้นจากรายชื่ออ้างอิง (auto-complete)")
@RestController
@RequestMapping("/api/v1/symbols")
@RequiredArgsConstructor
public class SymbolController {

    private final SymbolSearchService symbolSearchService;

    @GetMapping("/search")
    public List<SymbolSuggestionResponse> search(@RequestParam("q") String query,
                                                 @RequestParam(defaultValue = "10") int limit) {
        return symbolSearchService.search(query, Math.min(Math.max(limit, 1), 20)).stream()
                .map(QuoteMapper::toSuggestionResponse)
                .toList();
    }
}
