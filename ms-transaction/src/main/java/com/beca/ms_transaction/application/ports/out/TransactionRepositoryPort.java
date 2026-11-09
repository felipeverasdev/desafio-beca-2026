package com.beca.ms_transaction.application.ports.out;

import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    Page<Transaction> search(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate,
                             TransactionType type, Pageable pageable);
    List<Transaction> findForAnalysis(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate);
}

