package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.service.market.ExternalSymbolSearch;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบ auto-complete: รวมหุ้นในระบบ รายชื่ออ้างอิง และผลจาก Yahoo ไม่ซ้ำ และจัดอันดับตามความตรง
@ExtendWith(MockitoExtension.class)
class SymbolSearchServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private SymbolCatalog symbolCatalog;
    @Mock
    private ExternalSymbolSearch externalSymbolSearch;

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

        List<SymbolSuggestion> result = searchService.search("aa", 3);

        assertThat(result).extracting(SymbolSuggestion::symbol).containsExactly("AA", "AAOI", "AAPL");
        assertThat(result.get(2).assetId()).isEqualTo(7L);
        assertThat(result.get(0).assetId()).isNull();
        // ได้ครบตามจำนวนที่ขอแล้ว -> ไม่ต้องไปค้นจาก Yahoo
        verifyNoInteractions(externalSymbolSearch);
    }

    @Test
    @DisplayName("rklb ไม่อยู่ในรายชื่อของระบบ → ค้นต่อจาก Yahoo แล้วแสดง RKLB (ยังไม่อยู่ในระบบ assetId = null)")
    void fallsBackToExternalSearch() {
        when(assetRepository.findAll()).thenReturn(List.of());
        when(symbolCatalog.search(eq("RKLB"), anyInt())).thenReturn(List.of());
        when(externalSymbolSearch.search("RKLB", 10)).thenReturn(List.of(
                new SymbolInfo("RKLB", "Rocket Lab Corporation", AssetType.STOCK, "US")));

        List<SymbolSuggestion> result = searchService.search("rklb", 10);

        assertThat(result).extracting(SymbolSuggestion::symbol).containsExactly("RKLB");
        assertThat(result.get(0).exchange()).isEqualTo("US");
        assertThat(result.get(0).assetId()).isNull();
    }

    @Test
    @DisplayName("Yahoo ส่งหุ้นที่มีในรายชื่ออ้างอิงอยู่แล้วมาซ้ำ → แสดงครั้งเดียว")
    void externalResultsAreNotDuplicated() {
        when(assetRepository.findAll()).thenReturn(List.of());
        when(symbolCatalog.search(eq("EOS"), anyInt())).thenReturn(List.of(
                new SymbolInfo("EOSE", "Eos Energy Enterprises", AssetType.STOCK, "US")));
        when(externalSymbolSearch.search("EOS", 10)).thenReturn(List.of(
                new SymbolInfo("EOSE", "Eos Energy Enterprises, Inc.", AssetType.STOCK, "US"),
                new SymbolInfo("EOS", "Eaton Vance Enhanced Equity Income Fund II", AssetType.STOCK, "US")));

        List<SymbolSuggestion> result = searchService.search("eos", 10);

        assertThat(result).extracting(SymbolSuggestion::symbol).containsExactly("EOS", "EOSE");
        verify(externalSymbolSearch).search("EOS", 10);
    }

    @Test
    @DisplayName("คำค้นว่าง → ไม่ค้นเลย")
    void blankQuery() {
        assertThat(searchService.search("  ", 10)).isEmpty();
        verifyNoInteractions(assetRepository, symbolCatalog, externalSymbolSearch);
    }
}
