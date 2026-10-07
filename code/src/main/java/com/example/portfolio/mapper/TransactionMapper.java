package com.example.portfolio.mapper;

import com.example.portfolio.domain.entity.Transaction;
import com.example.portfolio.dto.response.TransactionResponse;

public class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(), transaction.getAsset().getId(), transaction.getAsset().getSymbol(),
                transaction.getType(), transaction.getQuantity(), transaction.getPrice(), transaction.getExecutedAt());
    }
}
