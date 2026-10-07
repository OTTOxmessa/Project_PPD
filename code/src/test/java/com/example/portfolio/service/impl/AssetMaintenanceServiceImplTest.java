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

    private static Asset ptt() {
        return Asset.builder().id(1L).symbol("PTT").name("PTT").assetType(AssetType.STOCK).exchange("SET").build();
    }

    @Test
    @DisplayName("update: แก้ชื่อ/ประเภท/ตลาดได้ (symbol ตัวพิมพ์เล็กถือว่าเป็นตัวเดิม)")
    void updateFields() {
        Asset existing = ptt();
        when(assetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        Asset changes = Asset.builder().symbol(" ptt ").name("PTT Public Company")
                .assetType(AssetType.ETF).exchange("SET100").build();

        Asset saved = service.update(1L, changes);

        assertThat(saved.getSymbol()).isEqualTo("PTT");
        assertThat(saved.getName()).isEqualTo("PTT Public Company");
        assertThat(saved.getAssetType()).isEqualTo(AssetType.ETF);
        assertThat(saved.getExchange()).isEqualTo("SET100");
    }

    @Test
    @DisplayName("update: เปลี่ยน symbol → IllegalStateException (409) ไม่บันทึก")
    void updateRejectsSymbolChange() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(ptt()));

        assertThatThrownBy(() -> service.update(1L, Asset.builder().symbol("AOT").name("x").build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AOT");
        verify(assetRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: ยังมีพอร์ตถือหรือมีธุรกรรม → IllegalStateException (409) ไม่ลบอะไรเลย")
    void deleteInUse() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(ptt()));
        when(assetRepository.isInUse(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PTT");
        verify(assetRepository, never()).delete(any());
        verify(assetRepository, never()).removeFromAllWatchlists(anyLong());
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    @DisplayName("delete: ไม่มีใครใช้ → ลบราคาย้อนหลังและ watchlist ก่อน แล้วค่อยลบสินทรัพย์")
    void deleteUnused() {
        Asset asset = ptt();
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
