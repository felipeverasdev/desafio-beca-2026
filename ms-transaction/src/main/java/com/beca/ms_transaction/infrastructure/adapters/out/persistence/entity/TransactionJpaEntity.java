package com.beca.ms_transaction.infrastructure.adapters.out.persistence.entity;

import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tb_transactions")
public class TransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionCategory category;

    @Column(length = 255)
    private String description;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    public TransactionJpaEntity() {
    }

    public TransactionJpaEntity(UUID id, UUID userId, TransactionType type, TransactionStatus status,
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

