package com.beca.ms_transaction.infrastructure.config.security;

import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");

            Throwable rootCause = authException;
            while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
                rootCause = rootCause.getCause();
            }

            if (rootCause instanceof ExternalServiceUnavailableException
                    || "Serviço de usuários indisponível".equals(authException.getMessage())
                    || (authException.getCause() != null && "Serviço de usuários indisponível".equals(authException.getCause().getMessage()))) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.getWriter().write("""
                        {"timestamp":"%s","status":503,"error":"Service Unavailable","message":"Serviço de usuários indisponível","path":"%s"}
                        """.formatted(Instant.now(), request.getRequestURI()));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("""
                        {"timestamp":"%s","status":401,"error":"Unauthorized","message":"Autenticação necessária ou credenciais inválidas","path":"%s"}
                        """.formatted(Instant.now(), request.getRequestURI()));
            }
            response.getWriter().flush();
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("""
                    {"timestamp":"%s","status":403,"error":"Forbidden","message":"%s","path":"%s"}
                    """.formatted(Instant.now(),
                    accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Acesso negado",
                    request.getRequestURI()));
            response.getWriter().flush();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentação Swagger e OpenAPI abertas
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Todas as rotas de transação requerem autenticação
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}

