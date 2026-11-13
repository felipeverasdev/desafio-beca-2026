package com.beca.ms_transaction.infrastructure.adapters.out.client.msuser;

import com.beca.ms_transaction.application.ports.out.UserAuthenticationPort;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import com.beca.ms_transaction.domain.models.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class MsUserAuthenticationAdapter implements UserAuthenticationPort {

    private static final Logger log = LoggerFactory.getLogger(MsUserAuthenticationAdapter.class);

    private final RestClient restClient;
    private final String msUsersUrl;

    public MsUserAuthenticationAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${services.ms-user.url:http://localhost:8081/api/v1/users}") String msUsersUrl) {
        this.msUsersUrl = msUsersUrl;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Optional<AuthenticatedUser> authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return Optional.empty();
        }

        try {
            AuthResponseDTO response = restClient.post()
                    .uri(msUsersUrl + "/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email.trim().toLowerCase(), "password", rawPassword))
                    .retrieve()
                    .body(AuthResponseDTO.class);

            if (response != null && response.id() != null) {
                return Optional.of(new AuthenticatedUser(response.id(), response.email()));
            }
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 404) {
                log.debug("ms-user rejeitou autenticação para {}: status {}", email, ex.getStatusCode());
                return Optional.empty();
            }
            log.error("ms-user retornou erro {} ao autenticar {}: {}", ex.getStatusCode(), email, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço de usuários indisponível");
        } catch (Exception ex) {
            log.error("Falha ao comunicar com ms-user durante autenticação de {}: {}", email, ex.getMessage());
            throw new ExternalServiceUnavailableException("Serviço de usuários indisponível");
        }
    }

    public record AuthResponseDTO(UUID id, String email) {}
}

