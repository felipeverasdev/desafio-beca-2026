package com.beca.ms_transaction.infrastructure.config.security;

import com.beca.ms_transaction.application.ports.out.UserAuthenticationPort;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import com.beca.ms_transaction.domain.models.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserAuthenticationPort userAuthenticationPort;

    public CustomAuthenticationProvider(UserAuthenticationPort userAuthenticationPort) {
        this.userAuthenticationPort = userAuthenticationPort;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";

        try {
            Optional<AuthenticatedUser> userOpt = userAuthenticationPort.authenticate(email, password);
            if (userOpt.isEmpty()) {
                throw new BadCredentialsException("Credenciais inválidas");
            }

            AuthenticatedUser user = userOpt.get();
            return new UsernamePasswordAuthenticationToken(user, password, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        } catch (ExternalServiceUnavailableException ex) {
            throw new AuthenticationServiceException("Serviço de usuários indisponível", ex);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

