package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponseDTO(
        UUID id,
        UUID userId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        TransactionCategory category,
        String description,
        UUID targetUserId,
        String rejectionReason,
        OffsetDateTime createdAt
) {
    public static TransactionResponseDTO fromDomain(Transaction domain) {
        return new TransactionResponseDTO(
                domain.getId(),
                domain.getUserId(),
                domain.getType(),
                domain.getStatus(),
                domain.getAmount(),
                domain.getCategory(),
                domain.getDescription(),
                domain.getTargetUserId(),
                domain.getRejectionReason(),
                domain.getCreatedAt()
        );
    }
}

