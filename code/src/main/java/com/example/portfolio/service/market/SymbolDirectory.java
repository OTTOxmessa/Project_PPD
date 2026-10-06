package com.example.portfolio.service.market;

import com.example.portfolio.domain.enums.AssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// SymbolCatalog ที่อ่านรายชื่อหุ้นอ้างอิงจาก resources/symbols.txt ครั้งเดียวตอน start
@Component
public class SymbolDirectory implements SymbolCatalog {

    private static final Logger log = LoggerFactory.getLogger(SymbolDirectory.class);

    private final List<SymbolInfo> symbols;

    public SymbolDirectory() {
        this.symbols = load();
        log.info("โหลดรายชื่อหุ้นอ้างอิง {} รายการ", symbols.size());
    }

    @Override
    public Optional<SymbolInfo> findBySymbol(String symbol) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return symbols.stream().filter(s -> s.symbol().equals(normalized)).findFirst();
    }

    @Override
    public List<SymbolInfo> search(String query, int limit) {
        String q = query.trim().toUpperCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }
        return symbols.stream()
                .filter(s -> SymbolMatcher.rank(s.symbol(), s.name(), q) < SymbolMatcher.NO_MATCH)
                .sorted(Comparator.comparingInt((SymbolInfo s) -> SymbolMatcher.rank(s.symbol(), s.name(), q))
                        .thenComparingInt(s -> s.symbol().length())
                        .thenComparing(SymbolInfo::symbol))
                .limit(limit)
                .toList();
    }

    private List<SymbolInfo> load() {
        List<SymbolInfo> result = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource("symbols.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length < 4) {
                    continue;
                }
                try {
                    result.add(new SymbolInfo(parts[0].trim().toUpperCase(Locale.ROOT), parts[1].trim(),
                            AssetType.valueOf(parts[2].trim()), parts[3].trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("ข้ามบรรทัดที่ประเภทไม่ถูกต้องใน symbols.txt: {}", line);
                }
            }
        } catch (IOException e) {
            log.warn("โหลด symbols.txt ไม่สำเร็จ: {}", e.getMessage());
        }
        return List.copyOf(result);
    }
}
