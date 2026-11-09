package com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi;

import com.beca.ms_transaction.application.ports.out.CurrencyExchangePort;
import com.beca.ms_transaction.domain.models.CurrencyQuote;
import com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi.dto.BrasilApiCotacaoItemDTO;
import com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi.dto.BrasilApiCotacaoResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class BrasilApiCurrencyExchangeAdapter implements CurrencyExchangePort {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiCurrencyExchangeAdapter.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestClient restClient;
    private final String brasilApiBaseUrl;

    public BrasilApiCurrencyExchangeAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.brasilapi.url:https://brasilapi.com.br/api/cambio/v1}") String brasilApiBaseUrl) {
        this.brasilApiBaseUrl = brasilApiBaseUrl;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Optional<CurrencyQuote> getQuote(String currency, LocalDate date) {
        if (currency == null || date == null) {
            return Optional.empty();
        }

        LocalDate targetDate = date;
        // Permite recuar até 5 dias para contornar finais de semana e feriados bancários
        for (int attempts = 0; attempts < 5; attempts++) {
            Optional<CurrencyQuote> quote = fetchQuoteForDate(currency, targetDate);
            if (quote.isPresent()) {
                return quote;
            }
            targetDate = targetDate.minusDays(1);
        }

        log.warn("Nenhuma cotação localizada para a moeda {} nos últimos 5 dias a partir de {}", currency, date);
        return Optional.empty();
    }

    private Optional<CurrencyQuote> fetchQuoteForDate(String currency, LocalDate date) {
        String formattedDate = date.format(DATE_FORMATTER);
        String url = String.format("%s/cotacao/%s/%s", brasilApiBaseUrl, currency.toUpperCase(), formattedDate);

        try {
            BrasilApiCotacaoResponseDTO response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.debug("BrasilAPI sem cotação ({}) para {} na data {}", resp.getStatusCode(), currency, formattedDate);
                    })
                    .body(BrasilApiCotacaoResponseDTO.class);

            if (response == null || response.cotacoes() == null || response.cotacoes().isEmpty()) {
                return Optional.empty();
            }

            // Seleciona a última cotação disponível do dia (geralmente Fechamento PTAX)
            BrasilApiCotacaoItemDTO latest = response.cotacoes().get(response.cotacoes().size() - 1);
            if (latest.cotacaoVenda() == null) {
                return Optional.empty();
            }

            return Optional.of(new CurrencyQuote(
                    currency.toUpperCase(),
                    latest.cotacaoVenda(),
                    date
            ));

        } catch (Exception ex) {
            log.warn("Falha de comunicação com BrasilAPI para moeda {} na data {}: {}", currency, formattedDate, ex.getMessage());
            return Optional.empty();
        }
    }
}

