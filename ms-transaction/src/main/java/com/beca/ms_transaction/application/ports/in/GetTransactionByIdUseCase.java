package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.Transaction;

import java.util.UUID;

public interface GetTransactionByIdUseCase {
    Transaction execute(UUID id, UUID authenticatedUserId);
}

