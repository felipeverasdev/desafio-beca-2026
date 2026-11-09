package com.beca.ms_transaction.application.ports.out;

import com.beca.ms_transaction.domain.models.CurrencyQuote;

import java.time.LocalDate;
import java.util.Optional;

public interface CurrencyExchangePort {
    Optional<CurrencyQuote> getQuote(String currency, LocalDate date);
}

