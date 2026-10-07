package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssetRequest(
        @NotBlank String symbol,
        @NotBlank String name,
        @NotNull AssetType assetType,
        String exchange
) {
}
