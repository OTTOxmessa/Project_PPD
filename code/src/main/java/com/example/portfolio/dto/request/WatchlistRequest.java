package com.example.portfolio.dto.request;

import jakarta.validation.constraints.NotNull;

public record WatchlistRequest(@NotNull(message = "กรุณาเลือกสินทรัพย์") Long assetId) {
}
