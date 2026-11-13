package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.Transaction;

public interface CreateTransactionUseCase {
    Transaction execute(CreateTransactionCommand command);
}

