package com.beca.ms_transaction.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyConversion(
        String currency,
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        LocalDate rateDate
) {
}

