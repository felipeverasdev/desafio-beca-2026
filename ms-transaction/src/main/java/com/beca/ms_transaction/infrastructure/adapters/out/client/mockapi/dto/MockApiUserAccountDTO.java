package com.beca.ms_transaction.infrastructure.adapters.out.client.mockapi.dto;

import java.math.BigDecimal;

public record MockApiUserAccountDTO(
        String id,
        String userId,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal transactionLimit
) {
}

