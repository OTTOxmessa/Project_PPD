package com.example.portfolio.dto.response;

import com.example.portfolio.domain.enums.RebalanceMethod;

import java.time.LocalDateTime;

public record RebalanceLogResponse(Long id, RebalanceMethod method, LocalDateTime triggeredAt, String details) {
}
