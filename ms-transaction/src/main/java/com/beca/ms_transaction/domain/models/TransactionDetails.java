package com.beca.ms_transaction.domain.models;

import java.util.List;

public record TransactionDetails(
        Transaction transaction,
        List<CurrencyConversion> conversions
) {
    public TransactionDetails {
        conversions = (conversions != null) ? List.copyOf(conversions) : List.of();
    }
}

