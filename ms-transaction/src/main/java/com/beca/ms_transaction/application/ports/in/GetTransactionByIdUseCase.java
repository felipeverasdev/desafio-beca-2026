package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.TransactionDetails;

import java.util.UUID;

public interface GetTransactionByIdUseCase {
    TransactionDetails execute(UUID id, UUID authenticatedUserId);
}
