package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.domain.models.CurrencyConversion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyConversionDTO(
        String currency,
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        LocalDate rateDate
) {
    public static CurrencyConversionDTO fromDomain(CurrencyConversion domain) {
        if (domain == null) {
            return null;
        }
        return new CurrencyConversionDTO(
                domain.currency(),
                domain.originalAmount(),
                domain.convertedAmount(),
                domain.exchangeRate(),
                domain.rateDate()
        );
    }
}

