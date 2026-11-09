package com.beca.ms_transaction.infrastructure.adapters.out.client.mockapi;

import com.beca.ms_transaction.application.ports.out.AccountAuthorizationPort;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import com.beca.ms_transaction.domain.models.AccountBalance;
import com.beca.ms_transaction.infrastructure.adapters.out.client.mockapi.dto.MockApiUserAccountDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MockApiAccountAuthorizationAdapter implements AccountAuthorizationPort {

    private static final Logger log = LoggerFactory.getLogger(MockApiAccountAuthorizationAdapter.class);

    private final RestClient restClient;
    private final String mockApiUrl;

    public MockApiAccountAuthorizationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.mockapi.url:https://6af23d8f42c6561ced39234e.mockapi.io/api/v1/transactions/users}") String mockApiUrl) {
        this.mockApiUrl = mockApiUrl;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Optional<AccountBalance> findAccountBalance(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }

        try {
            List<MockApiUserAccountDTO> accounts = restClient.get()
                    .uri(mockApiUrl + "?userId={userId}", userId.toString())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("MockAPI retornou status {} ao consultar usuário {}", resp.getStatusCode(), userId);
                    })
                    .body(new ParameterizedTypeReference<List<MockApiUserAccountDTO>>() {});

            if (accounts == null || accounts.isEmpty()) {
                return Optional.empty();
            }

            MockApiUserAccountDTO account = accounts.get(0);
            return Optional.of(new AccountBalance(
                    userId,
                    account.balance(),
                    account.dailyLimit(),
                    account.transactionLimit()
            ));

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("Erro retornado pelo MockAPI ao consultar userId {}: {}", userId, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço autorizador (MockAPI) retornou erro: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Falha de comunicação com o MockAPI para userId {}: {}", userId, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço autorizador (MockAPI) indisponível no momento");
        }
    }
}

