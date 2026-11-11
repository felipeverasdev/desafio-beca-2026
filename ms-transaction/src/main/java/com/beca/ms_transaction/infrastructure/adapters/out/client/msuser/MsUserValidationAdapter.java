package com.beca.ms_transaction.infrastructure.adapters.out.client.msuser;

import com.beca.ms_transaction.application.ports.out.UserValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class MsUserValidationAdapter implements UserValidationPort {

    private static final Logger log = LoggerFactory.getLogger(MsUserValidationAdapter.class);

    private final RestClient restClient;
    private final String msUsersUrl;

    public MsUserValidationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.ms-user.url:http://localhost:8081/api/v1/users}") String msUsersUrl) {
        this.msUsersUrl = msUsersUrl;
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
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.debug("ms-user retornou {} para userId {}", resp.getStatusCode(), userId);
                    })
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (Exception ex) {
            log.warn("Falha ao consultar usuário {} no ms-user ({}): {}", userId, msUsersUrl, ex.getMessage());
            return false;
        }
    }
}

