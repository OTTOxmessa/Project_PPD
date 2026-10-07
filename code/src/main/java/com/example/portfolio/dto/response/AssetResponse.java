package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.AssetType;

public record AssetResponse(Long id, String symbol, String name, AssetType assetType, String exchange) {
}
