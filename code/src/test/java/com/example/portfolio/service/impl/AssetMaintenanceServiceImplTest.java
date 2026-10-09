package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// แก้ไข/ลบสินทรัพย์: กันการเปลี่ยน symbol และกันการลบสินทรัพย์ที่ยังถูกใช้อยู่
@ExtendWith(MockitoExtension.class)
class AssetMaintenanceServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private AssetMaintenanceServiceImpl service;

    private static Asset aapl() {
        return Asset.builder().id(1L).symbol("AAPL").name("Apple Inc.").assetType(AssetType.STOCK).exchange("US").build();
    }

    @Test
    @DisplayName("update: แก้ชื่อ/ประเภทได้ ตลาด NASDAQ เก็บเป็น US (symbol ตัวพิมพ์เล็กถือว่าเป็นตัวเดิม)")
    void updateFields() {
        Asset existing = aapl();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        Asset changes = Asset.builder().symbol(" aapl ").name("Apple Incorporated")
                .assetType(AssetType.ETF).exchange("NASDAQ").build();

        Asset saved = service.update(1L, changes);

        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getName()).isEqualTo("Apple Incorporated");
        assertThat(saved.getAssetType()).isEqualTo(AssetType.ETF);
        assertThat(saved.getExchange()).isEqualTo("US");
    }

    @Test
    @DisplayName("update: เปลี่ยนเป็นตลาดอื่น (SET) หรือประเภทที่ไม่รองรับ (CRYPTO) → IllegalArgumentException (400) ไม่บันทึก")
    void updateRejectsNonUsMarket() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(aapl()));

        assertThatThrownBy(() -> service.update(1L, Asset.builder().symbol("AAPL").name("x")
                .assetType(AssetType.STOCK).exchange("SET").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SET");
        assertThatThrownBy(() -> service.update(1L, Asset.builder().symbol("AAPL").name("x")
                .assetType(AssetType.CRYPTO).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CRYPTO");
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: เปลี่ยน symbol → IllegalStateException (409) ไม่บันทึก")
    void updateRejectsSymbolChange() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(aapl()));

        assertThatThrownBy(() -> service.update(1L, Asset.builder().symbol("MSFT").name("x").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSFT");
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: ยังมีพอร์ตถือหรือมีธุรกรรม → IllegalStateException (409) ไม่ลบอะไรเลย")
    void deleteInUse() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(aapl()));
        when(assetRepository.isInUse(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AAPL");
        verify(assetRepository, never()).delete(any());
        verify(assetRepository, never()).removeFromAllWatchlists(anyLong());
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    @DisplayName("delete: ไม่มีใครใช้ → ลบราคาย้อนหลังและ watchlist ก่อน แล้วค่อยลบสินทรัพย์")
    void deleteUnused() {
        Asset asset = aapl();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.isInUse(1L)).thenReturn(false);

        service.delete(1L);

        InOrder order = inOrder(priceHistoryRepository, assetRepository);
        order.verify(priceHistoryRepository).purgeByAssetId(1L);
        order.verify(assetRepository).removeFromAllWatchlists(1L);
        order.verify(assetRepository).delete(asset);
    }

    @Test
    @DisplayName("delete: ไม่มีสินทรัพย์นี้ → ResourceNotFoundException (404)")
    void deleteNotFound() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
