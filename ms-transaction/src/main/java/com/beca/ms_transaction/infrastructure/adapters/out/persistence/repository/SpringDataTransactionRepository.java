package com.beca.ms_transaction.infrastructure.adapters.out.persistence.repository;

import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;
import com.beca.ms_transaction.infrastructure.adapters.out.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID>,
        JpaSpecificationExecutor<TransactionJpaEntity> {

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM TransactionJpaEntity t
            WHERE t.userId = :userId
              AND t.status = :status
              AND t.type IN :types
              AND t.createdAt >= :startDate
              AND t.createdAt < :endDate
            """)
    BigDecimal sumOutflows(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status,
            @Param("types") Collection<TransactionType> types,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
