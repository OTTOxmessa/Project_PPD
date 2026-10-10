package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.Portfolio;
import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.domain.enums.TransactionType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PortfolioRepository;
import com.example.portfolio.repository.TransactionRepository;
import com.example.portfolio.service.HoldingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// ทดสอบว่าการบันทึกธุรกรรมอัปเดต holding ไปพร้อมกัน และไม่บันทึกถ้า holding ปฏิเสธ
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private HoldingService holdingService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private void portfolioAndAssetExist() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.of(Portfolio.builder().id(1L).build()));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(Asset.builder().id(10L).symbol("MSFT").build()));
    }

    @Test
    @DisplayName("BUY: อัปเดต holding แล้วบันทึก transaction โดยใช้เวลาปัจจุบันถ้าไม่ระบุ")
    void buyUpdatesHoldingAndSaves() {
        portfolioAndAssetExist();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime before = LocalDateTime.now();

        Transaction saved = transactionService.record(1L, 10L, TransactionType.BUY,
                new BigDecimal("100"), new BigDecimal("60"), null);

        verify(holdingService).applyTransaction(1L, 10L, TransactionType.BUY, new BigDecimal("100"), new BigDecimal("60"));
        assertThat(saved.getType()).isEqualTo(TransactionType.BUY);
        assertThat(saved.getAsset().getSymbol()).isEqualTo("MSFT");
        assertThat(saved.getExecutedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("ระบุวันที่ย้อนหลังได้: ใช้ executedAt ที่ส่งมา")
    void keepsGivenExecutedAt() {
        portfolioAndAssetExist();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime past = LocalDateTime.of(2026, 1, 5, 12, 0);

        Transaction saved = transactionService.record(1L, 10L, TransactionType.SELL,
                BigDecimal.ONE, BigDecimal.TEN, past);

        assertThat(saved.getExecutedAt()).isEqualTo(past);
    }

    @Test
    @DisplayName("DIVIDEND: บันทึก transaction แต่ไม่แตะ holding")
    void dividendDoesNotTouchHolding() {
        portfolioAndAssetExist();
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.record(1L, 10L, TransactionType.DIVIDEND, BigDecimal.ONE, new BigDecimal("2.5"), null);

        verifyNoInteractions(holdingService);
    }

    @Test
    @DisplayName("ขายเกินจำนวน: holding โยน exception → ไม่บันทึก transaction")
    void oversellIsNotSaved() {
        portfolioAndAssetExist();
        when(holdingService.applyTransaction(anyLong(), anyLong(), any(), any(), any()))
                .thenThrow(new IllegalStateException("ขายเกินจำนวนที่ถืออยู่"));

        assertThatThrownBy(() -> transactionService.record(1L, 10L, TransactionType.SELL,
                new BigDecimal("999"), BigDecimal.TEN, null))
                .isInstanceOf(IllegalStateException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("พอร์ตไม่มีอยู่ → ResourceNotFoundException")
    void unknownPortfolio() {
        when(portfolioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.record(1L, 10L, TransactionType.BUY,
                BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(holdingService);
    }

    private static Transaction existing() {
        return Transaction.builder().id(7L)
                .portfolio(Portfolio.builder().id(1L).build())
                .asset(Asset.builder().id(10L).symbol("MSFT").build())
                .type(TransactionType.BUY).quantity(new BigDecimal("10")).price(new BigDecimal("100"))
                .executedAt(LocalDateTime.of(2026, 3, 1, 9, 30)).build();
    }

    @Test
    @DisplayName("update: แก้จำนวน/ราคา วันเดิม → คงเวลาเดิม แล้วคำนวณ holding ใหม่จากประวัติ")
    void updateRecalculatesHolding() {
        Transaction tx = existing();
        List<Transaction> history = List.of(tx);
        when(transactionRepository.findByIdAndPortfolioId(7L, 1L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(tx)).thenReturn(tx);
        when(transactionRepository.findByPortfolioIdAndAssetIdOrderByExecutedAtAscIdAsc(1L, 10L)).thenReturn(history);

        Transaction updated = transactionService.update(1L, 7L, TransactionType.BUY,
                new BigDecimal("12"), new BigDecimal("95"), LocalDateTime.of(2026, 3, 1, 12, 0));

        assertThat(updated.getQuantity()).isEqualByComparingTo("12");
        assertThat(updated.getPrice()).isEqualByComparingTo("95");
        assertThat(updated.getExecutedAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 9, 30));
        verify(holdingService).recalculate(1L, 10L, history);
    }

    @Test
    @DisplayName("update: เปลี่ยนวันที่ → ใช้วันที่ใหม่")
    void updateChangesDate() {
        Transaction tx = existing();
        when(transactionRepository.findByIdAndPortfolioId(7L, 1L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(tx)).thenReturn(tx);

        transactionService.update(1L, 7L, TransactionType.BUY, BigDecimal.TEN, BigDecimal.TEN,
                LocalDateTime.of(2026, 2, 15, 12, 0));

        assertThat(tx.getExecutedAt()).isEqualTo(LocalDateTime.of(2026, 2, 15, 12, 0));
    }

    @Test
    @DisplayName("update: รายการไม่อยู่ในพอร์ตนี้ → ResourceNotFoundException และไม่แตะ holding")
    void updateForeignTransaction() {
        when(transactionRepository.findByIdAndPortfolioId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.update(1L, 7L, TransactionType.BUY,
                BigDecimal.ONE, BigDecimal.ONE, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(holdingService);
    }

    @Test
    @DisplayName("delete: ลบรายการแล้วคำนวณ holding ของหุ้นนั้นใหม่")
    void deleteRecalculatesHolding() {
        Transaction tx = existing();
        when(transactionRepository.findByIdAndPortfolioId(7L, 1L)).thenReturn(Optional.of(tx));
        when(transactionRepository.findByPortfolioIdAndAssetIdOrderByExecutedAtAscIdAsc(1L, 10L)).thenReturn(List.of());

        transactionService.delete(1L, 7L);

        verify(transactionRepository).delete(tx);
        verify(holdingService).recalculate(1L, 10L, List.of());
    }
}
