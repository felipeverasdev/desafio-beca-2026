package com.beca.ms_transaction.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyQuote(
        String currency,
        BigDecimal rate,
        LocalDate quoteDate
) {
}

