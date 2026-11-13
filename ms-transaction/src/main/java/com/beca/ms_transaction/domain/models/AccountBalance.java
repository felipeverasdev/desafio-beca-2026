package com.beca.ms_transaction.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalance(
        UUID userId,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal transactionLimit
) {
}

