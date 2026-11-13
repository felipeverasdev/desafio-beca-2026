package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

public interface ExportTransactionsCsvUseCase {
    byte[] execute(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type);
}

