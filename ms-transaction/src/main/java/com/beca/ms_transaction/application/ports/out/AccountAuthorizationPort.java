package com.beca.ms_transaction.application.ports.out;

import com.beca.ms_transaction.domain.models.AccountBalance;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountAuthorizationPort {
    Optional<AccountBalance> findAccountBalance(UUID userId);
    void updateBalance(UUID userId, BigDecimal newBalance);
}

