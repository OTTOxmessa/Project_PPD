package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.service.market.SymbolCatalog;
import com.example.portfolio.service.market.SymbolInfo;
import com.example.portfolio.service.market.SymbolSuggestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบ auto-complete: รวมหุ้นในระบบกับรายชื่ออ้างอิง ไม่ซ้ำ และจัดอันดับตามความตรง
@ExtendWith(MockitoExtension.class)
class SymbolSearchServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private SymbolCatalog symbolCatalog;

    @InjectMocks
    private SymbolSearchServiceImpl searchService;

    @Test
    @DisplayName("aa → AA (ตรงทั้งคำ) มาก่อน, ที่เหลือเรียงตามความยาวและตัวอักษร, หุ้นในระบบมี assetId และไม่ซ้ำ")
    void ranksAndMerges() {
        when(assetRepository.findAll()).thenReturn(List.of(
                Asset.builder().id(7L).symbol("AAPL").name("Apple Inc.").assetType(AssetType.STOCK).exchange("US").build()));
        when(symbolCatalog.search(eq("AA"), anyInt())).thenReturn(List.of(
                new SymbolInfo("AA", "Alcoa Corporation", AssetType.STOCK, "US"),
                new SymbolInfo("AAPL", "Apple Inc.", AssetType.STOCK, "US"),
                new SymbolInfo("AAOI", "Applied Optoelectronics", AssetType.STOCK, "US")));

        List<SymbolSuggestion> result = searchService.search("aa", 10);

        assertThat(result).extracting(SymbolSuggestion::symbol).containsExactly("AA", "AAOI", "AAPL");
        assertThat(result.get(2).assetId()).isEqualTo(7L);
        assertThat(result.get(0).assetId()).isNull();
    }

    @Test
    @DisplayName("คำค้นว่าง → ไม่ค้นเลย")
    void blankQuery() {
        assertThat(searchService.search("  ", 10)).isEmpty();
        verifyNoInteractions(assetRepository, symbolCatalog);
    }
}
