package com.beca.ms_transaction.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Transaction {

    private UUID id;
    private UUID userId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private TransactionCategory category;
    private String description;
    private UUID targetUserId;
    private String rejectionReason;
    private OffsetDateTime createdAt;

    public Transaction() {
    }

    public Transaction(UUID id, UUID userId, TransactionType type, TransactionStatus status,
                       BigDecimal amount, TransactionCategory category, String description,
                       UUID targetUserId, String rejectionReason, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.targetUserId = targetUserId;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public static Transaction createApproved(UUID userId, TransactionType type, BigDecimal amount,
                                             TransactionCategory category, String description, UUID targetUserId) {
        return new Transaction(
                null,
                userId,
                type,
                TransactionStatus.APPROVED,
                amount,
                category != null ? category : TransactionCategory.OUTROS,
                description,
                targetUserId,
                null,
                null
        );
    }

    public static Transaction createRejected(UUID userId, TransactionType type, BigDecimal amount,
                                             TransactionCategory category, String description,
                                             UUID targetUserId, String rejectionReason) {
        return new Transaction(
                null,
                userId,
                type,
                TransactionStatus.REJECTED,
                amount,
                category != null ? category : TransactionCategory.OUTROS,
                description,
                targetUserId,
                rejectionReason,
                null
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

