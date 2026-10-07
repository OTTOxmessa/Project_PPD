package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.service.SymbolSearchService;
import com.example.portfolio.service.market.SymbolCatalog;
import com.example.portfolio.service.market.SymbolInfo;
import com.example.portfolio.service.market.SymbolMatcher;
import com.example.portfolio.service.market.SymbolSuggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// แยกออกจาก AssetServiceImpl: งานค้นหา/จัดอันดับเป็นคนละหน้าที่กับการจัดการข้อมูลสินทรัพย์ (SRP)
@Service
@RequiredArgsConstructor
public class SymbolSearchServiceImpl implements SymbolSearchService {

    private final AssetRepository assetRepository;
    private final SymbolCatalog symbolCatalog;

    private record Ranked(int rank, SymbolSuggestion suggestion) {
    }

    @Override
    public List<SymbolSuggestion> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }

        List<Ranked> ranked = new ArrayList<>();
        Set<String> inSystem = new HashSet<>();

        // 1) หุ้นที่อยู่ในระบบแล้ว (ตาราง assets มีขนาดเล็ก จึงกรองในหน่วยความจำได้)
        for (Asset a : assetRepository.findAll()) {
            int rank = SymbolMatcher.rank(a.getSymbol(), a.getName(), q);
            if (rank < SymbolMatcher.NO_MATCH) {
                ranked.add(new Ranked(rank, new SymbolSuggestion(
                        a.getSymbol(), a.getName(), a.getAssetType(), a.getExchange(), a.getId())));
                inSystem.add(a.getSymbol());
            }
        }
        // 2) รายชื่ออ้างอิงที่ยังไม่อยู่ในระบบ
        for (SymbolInfo s : symbolCatalog.search(q, limit * 2)) {
            if (!inSystem.contains(s.symbol())) {
                ranked.add(new Ranked(SymbolMatcher.rank(s.symbol(), s.name(), q),
                        new SymbolSuggestion(s.symbol(), s.name(), s.assetType(), s.exchange(), null)));
            }
        }

        return ranked.stream()
                .sorted(Comparator.comparingInt(Ranked::rank)
                        .thenComparingInt((Ranked r) -> r.suggestion().symbol().length())
                        .thenComparing(r -> r.suggestion().symbol()))
                .limit(limit)
                .map(Ranked::suggestion)
                .toList();
    }
}
