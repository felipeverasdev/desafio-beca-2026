package com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi;

import com.beca.ms_transaction.domain.models.CurrencyQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiCurrencyExchangeAdapterTest {

    private static final String BASE_URL = "https://brasilapi.com.br/api/cambio/v1";
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    private MockRestServiceServer mockServer;
    private BrasilApiCurrencyExchangeAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrasilApiCurrencyExchangeAdapter(builder, BASE_URL);
    }

    @Test
    @DisplayName("Regra 1: Se data for hoje ou futura, deve consultar o dia anterior")
    void shouldQueryPreviousDayWhenDateIsTodayOrFuture() {
        LocalDate today = LocalDate.now(ZONE_SP);
        LocalDate yesterday = today.minusDays(1);

        String jsonResponse = """
                {
                  "moeda": "USD",
                  "data": "%s",
                  "cotacoes": [
                    {
                      "cotacao_compra": 5.50,
                      "cotacao_venda": 5.55,
                      "data_hora_cotacao": "%s 13:06:00",
                      "tipo_boletim": "FECHAMENTO PTAX"
                    }
                  ]
                }
                """.formatted(yesterday, yesterday);

        mockServer.expect(requestTo(BASE_URL + "/cotacao/USD/" + yesterday))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<CurrencyQuote> result = adapter.getQuote("USD", today);

        mockServer.verify();
        assertTrue(result.isPresent());
        assertEquals("USD", result.get().currency());
        assertEquals(new BigDecimal("5.55"), result.get().rate());
        assertEquals(yesterday, result.get().quoteDate());
    }

    @Test
    @DisplayName("Regra 2: Deve usar a data retornada na resposta JSON como quoteDate (ex: fim de semana devolve sexta)")
    void shouldUseDateFromResponseBodyAsQuoteDate() {
        LocalDate weekendDate = LocalDate.of(2026, 11, 8); // Domingo
        LocalDate fridayDate = LocalDate.of(2026, 11, 6);  // Sexta-feira

        String jsonResponse = """
                {
                  "moeda": "USD",
                  "data": "2026-11-06",
                  "cotacoes": [
                    {
                      "cotacao_compra": 5.48,
                      "cotacao_venda": 5.50,
                      "data_hora_cotacao": "2026-11-06 13:06:00",
                      "tipo_boletim": "FECHAMENTO PTAX"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/cotacao/USD/2026-11-08"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<CurrencyQuote> result = adapter.getQuote("USD", weekendDate);

        mockServer.verify();
        assertTrue(result.isPresent());
        assertEquals(fridayDate, result.get().quoteDate());
        assertEquals(new BigDecimal("5.50"), result.get().rate());
    }

    @Test
    @DisplayName("Regra 3: Falha na requisição da BrasilAPI não quebra a aplicação e retorna Optional.empty()")
    void shouldReturnEmptyWhenBrasilApiFails() {
        LocalDate pastDate = LocalDate.of(2026, 11, 1);

        mockServer.expect(requestTo(BASE_URL + "/cotacao/USD/2026-11-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        // Permitimos o recuo das tentativas
        for (int i = 1; i <= 4; i++) {
            mockServer.expect(requestTo(BASE_URL + "/cotacao/USD/" + pastDate.minusDays(i)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withBadRequest());
        }

        Optional<CurrencyQuote> result = adapter.getQuote("USD", pastDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

