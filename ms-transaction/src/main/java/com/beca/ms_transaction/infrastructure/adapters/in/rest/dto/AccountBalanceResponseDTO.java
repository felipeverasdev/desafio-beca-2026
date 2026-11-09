package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.domain.models.AccountBalance;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponseDTO(
        UUID userId,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal transactionLimit
) {
    public static AccountBalanceResponseDTO fromDomain(AccountBalance domain) {
        if (domain == null) {
            return null;
        }
        return new AccountBalanceResponseDTO(
                domain.userId(),
                domain.balance(),
                domain.dailyLimit(),
                domain.transactionLimit()
        );
    }
}

