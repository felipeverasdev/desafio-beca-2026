package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.domain.models.CurrencyConversion;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
        OffsetDateTime createdAt,
        List<CurrencyConversionDTO> conversions
) {
    public TransactionResponseDTO(UUID id, UUID userId, TransactionType type, TransactionStatus status,
                                  BigDecimal amount, TransactionCategory category, String description,
                                  UUID targetUserId, String rejectionReason, OffsetDateTime createdAt) {
        this(id, userId, type, status, amount, category, description, targetUserId, rejectionReason, createdAt, List.of());
    }

    public static TransactionResponseDTO fromDomain(Transaction domain) {
        return fromDomain(domain, List.of());
    }

    public static TransactionResponseDTO fromDomain(Transaction domain, List<CurrencyConversion> domainConversions) {
        List<CurrencyConversionDTO> conversionDTOs = (domainConversions != null && !domainConversions.isEmpty()) ?
                domainConversions.stream().map(CurrencyConversionDTO::fromDomain).toList() : List.of();

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
                domain.getCreatedAt(),
                conversionDTOs
        );
    }
}
