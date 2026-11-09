package com.beca.ms_transaction.application.ports.out;

import com.beca.ms_transaction.domain.models.AccountBalance;

import java.util.Optional;
import java.util.UUID;

public interface AccountAuthorizationPort {
    Optional<AccountBalance> findAccountBalance(UUID userId);
}

