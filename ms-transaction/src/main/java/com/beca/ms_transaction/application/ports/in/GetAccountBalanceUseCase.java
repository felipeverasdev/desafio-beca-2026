package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.AccountBalance;

import java.util.UUID;

public interface GetAccountBalanceUseCase {
    AccountBalance execute(UUID userId);
}

