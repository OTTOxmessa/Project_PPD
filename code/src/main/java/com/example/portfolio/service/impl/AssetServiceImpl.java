package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.PriceHistory;
import com.example.portfolio.domain.enums.AssetType;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.AssetService;
import com.example.portfolio.service.PriceHistoryService;
import com.example.portfolio.service.market.SymbolCatalog;
import com.example.portfolio.service.market.SymbolInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SymbolCatalog symbolCatalog;
    private final PriceHistoryService priceHistoryService;

    @Override
    public List<Asset> getAll() {
        return assetRepository.findAll();
    }

    @Override
    public Asset getById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }

    @Override
    @Transactional
    public Asset create(Asset asset) {
        asset.setSymbol(normalize(asset.getSymbol()));
        if (assetRepository.existsBySymbol(asset.getSymbol())) {
            throw new IllegalStateException("มีสินทรัพย์ symbol " + asset.getSymbol() + " อยู่แล้ว");
        }
        Asset saved = assetRepository.save(asset);
        priceHistoryService.ensureHistory(saved, null); // ลอง Yahoo ก่อน ไม่ได้ค่อยใช้ข้อมูลจำลอง
        return saved;
    }

    @Override
    @Transactional
    public Asset ensure(String symbol, String name, AssetType assetType, String exchange, BigDecimal referencePrice) {
        String normalized = normalize(symbol);
        Asset asset = assetRepository.findBySymbol(normalized).orElseGet(() -> {
            Optional<SymbolInfo> info = symbolCatalog.findBySymbol(normalized);
            String resolvedName = hasText(name) ? name.trim() : info.map(SymbolInfo::name).orElse(null);
            if (resolvedName == null) {
                throw new IllegalArgumentException(
                        "ไม่พบ " + normalized + " ในรายชื่อหุ้นอ้างอิง — กรุณาเพิ่มในหน้าสินทรัพย์พร้อมระบุชื่อ");
            }
            AssetType resolvedType = assetType != null ? assetType
                    : info.map(SymbolInfo::assetType).orElse(AssetType.STOCK);
            String resolvedExchange = hasText(exchange) ? exchange.trim()
                    : info.map(SymbolInfo::exchange).orElse(null);
            return assetRepository.save(Asset.builder()
                    .symbol(normalized)
                    .name(resolvedName)
                    .assetType(resolvedType)
                    .exchange(resolvedExchange)
                    .build());
        });
        // ยังไม่มีราคา -> ลองดึงราคาจริงจาก Yahoo, ไม่สำเร็จใช้ข้อมูลจำลอง (มีข้อมูลแล้วจะไม่ทำอะไร)
        priceHistoryService.ensureHistory(asset, referencePrice);
        return asset;
    }

    @Override
    public List<PriceHistory> getPriceHistory(Long assetId, LocalDate from, LocalDate to) {
        getById(assetId);
        return priceHistoryRepository.findByAssetIdAndPriceDateBetweenOrderByPriceDateAsc(assetId, from, to);
    }

    private static String normalize(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
