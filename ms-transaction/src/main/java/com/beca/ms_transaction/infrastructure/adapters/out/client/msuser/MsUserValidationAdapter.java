package com.beca.ms_transaction.infrastructure.adapters.out.client.msuser;

import com.beca.ms_transaction.application.ports.out.UserValidationPort;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class MsUserValidationAdapter implements UserValidationPort {

    private static final Logger log = LoggerFactory.getLogger(MsUserValidationAdapter.class);

    private final RestClient restClient;
    private final String msUsersUrl;
    private final String serviceToken;

    public MsUserValidationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.ms-user.url:http://localhost:8081/api/v1/users}") String msUsersUrl,
            @Value("${services.ms-user.token}") String serviceToken) {
        this.msUsersUrl = msUsersUrl;
        this.serviceToken = serviceToken;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean existsAndActive(UUID userId) {
        if (userId == null) {
            return false;
        }

        try {
            return restClient.get()
                    .uri(msUsersUrl + "/{id}/exists", userId)
                    .header("X-Service-Token", serviceToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        log.debug("ms-user retornou 404 para userId {}", userId);
                    })
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            log.error("ms-user retornou status {} ao validar userId {}: {}", ex.getStatusCode(), userId, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço de usuários indisponível");
        } catch (Exception ex) {
            log.error("Falha ao conectar no ms-user ({}) para userId {}: {}", msUsersUrl, userId, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço de usuários indisponível");
        }
    }
}

