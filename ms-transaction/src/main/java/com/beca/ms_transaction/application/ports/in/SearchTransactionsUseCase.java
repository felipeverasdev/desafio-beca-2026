package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SearchTransactionsUseCase {
    Page<Transaction> execute(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate,
                              TransactionType type, Pageable pageable);
}

