package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssetRequest(
        @NotBlank(message = "กรุณากรอก symbol") String symbol,
        @NotBlank(message = "กรุณากรอกชื่อสินทรัพย์") String name,
        @NotNull(message = "กรุณาเลือกประเภทสินทรัพย์") AssetType assetType,
        String exchange
) {
}
