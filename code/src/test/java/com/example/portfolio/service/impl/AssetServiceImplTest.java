package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.PriceHistoryService;
import com.example.portfolio.service.market.SymbolCatalog;
import com.example.portfolio.service.market.SymbolInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ทดสอบการเพิ่มสินทรัพย์: รับเฉพาะหุ้น/ETF ตลาดสหรัฐฯ และเก็บชื่อตลาดเป็น "US"
@ExtendWith(MockitoExtension.class)
class AssetServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private SymbolCatalog symbolCatalog;
    @Mock
    private PriceHistoryService priceHistoryService;

    @InjectMocks
    private AssetServiceImpl assetService;

    @Test
    @DisplayName("create: ตลาด NASDAQ → เก็บเป็น US, symbol เป็นตัวพิมพ์ใหญ่ แล้วดึงราคาย้อนหลัง")
    void createUsStock() {
        when(assetRepository.existsBySymbol("MSFT")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        Asset input = Asset.builder().symbol(" msft ").name("Microsoft").assetType(AssetType.STOCK).exchange("NASDAQ").build();

        Asset saved = assetService.create(input);

        assertThat(saved.getSymbol()).isEqualTo("MSFT");
        assertThat(saved.getExchange()).isEqualTo("US");
        verify(priceHistoryService).ensureHistory(saved, null);
    }

    @Test
    @DisplayName("create: หุ้นตลาด SET → IllegalArgumentException (400) และไม่บันทึก")
    void createRejectsThaiStock() {
        Asset input = Asset.builder().symbol("PTT").name("PTT PCL").assetType(AssetType.STOCK).exchange("SET").build();

        assertThatThrownBy(() -> assetService.create(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SET");
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("ensure: symbol ที่อยู่ในรายชื่ออ้างอิง → สร้างจากข้อมูลในรายชื่อ ตลาด US")
    void ensureFromCatalog() {
        when(assetRepository.findBySymbol("AAPL")).thenReturn(Optional.empty());
        when(symbolCatalog.findBySymbol("AAPL"))
                .thenReturn(Optional.of(new SymbolInfo("AAPL", "Apple Inc.", AssetType.STOCK, "US")));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Asset asset = assetService.ensure("aapl", null, null, null, null);

        assertThat(asset.getName()).isEqualTo("Apple Inc.");
        assertThat(asset.getAssetType()).isEqualTo(AssetType.STOCK);
        assertThat(asset.getExchange()).isEqualTo("US");
        verify(priceHistoryService).ensureHistory(asset, null);
    }

    @Test
    @DisplayName("ensure: คริปโต → IllegalArgumentException (400) ไม่บันทึกและไม่ดึงราคา")
    void ensureRejectsCrypto() {
        when(assetRepository.findBySymbol("BTC")).thenReturn(Optional.empty());
        when(symbolCatalog.findBySymbol("BTC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.ensure("BTC", "Bitcoin", AssetType.CRYPTO, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CRYPTO");
        verify(assetRepository, never()).save(any());
        verify(priceHistoryService, never()).ensureHistory(any(), isNull());
    }

    @Test
    @DisplayName("ensure: สินทรัพย์ที่มีอยู่แล้ว → คืนตัวเดิม ไม่สร้างซ้ำ")
    void ensureExisting() {
        Asset existing = Asset.builder().id(3L).symbol("VOO").name("Vanguard S&P 500 ETF")
                .assetType(AssetType.ETF).exchange("US").build();
        when(assetRepository.findBySymbol("VOO")).thenReturn(Optional.of(existing));

        assertThat(assetService.ensure("VOO", null, null, null, null)).isSameAs(existing);
        verify(assetRepository, never()).save(any());
    }
}
