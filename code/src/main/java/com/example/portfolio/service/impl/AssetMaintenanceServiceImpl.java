package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.PriceHistoryRepository;
import com.example.portfolio.service.AssetMaintenanceService;
import com.example.portfolio.service.market.UsMarket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AssetMaintenanceServiceImpl implements AssetMaintenanceService {

    private final AssetRepository assetRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional
    public Asset update(Long id, Asset changes) {
        Asset asset = find(id);
        String requestedSymbol = changes.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (!requestedSymbol.equals(asset.getSymbol())) {
            throw new IllegalStateException("เปลี่ยน symbol จาก " + asset.getSymbol() + " เป็น " + requestedSymbol
                    + " ไม่ได้ เพราะราคาย้อนหลังผูกกับ symbol เดิม — ให้เพิ่มเป็นสินทรัพย์ใหม่แทน");
        }
        String exchange = UsMarket.requireSupported(asset.getSymbol(), changes.getAssetType(), changes.getExchange());
        asset.setName(changes.getName());
        asset.setAssetType(changes.getAssetType());
        asset.setExchange(exchange);
        return assetRepository.save(asset);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Asset asset = find(id);
        // ตรวจก่อนลบ เพื่อตอบ 409 พร้อมเหตุผลที่อ่านเข้าใจได้ (ฐานข้อมูลมี FK แบบ RESTRICT กันไว้อีกชั้น)
        if (assetRepository.isInUse(id)) {
            throw new IllegalStateException("ลบ " + asset.getSymbol()
                    + " ไม่ได้ เพราะยังมีพอร์ตถืออยู่ หรือมีธุรกรรม/alert/เป้าหมายสัดส่วนอ้างถึง");
        }
        // ข้อมูลที่เป็นของสินทรัพย์ตัวนี้เท่านั้น ลบตามไปด้วย (ตรงกับ ON DELETE CASCADE ใน schema.sql)
        priceHistoryRepository.purgeByAssetId(id);
        assetRepository.removeFromAllWatchlists(id);
        assetRepository.delete(asset);
    }

    private Asset find(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }
}
