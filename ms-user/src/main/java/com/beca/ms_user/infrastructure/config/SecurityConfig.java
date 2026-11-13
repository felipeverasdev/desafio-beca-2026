package com.beca.ms_user.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("""
                    {"timestamp":"%s","status":401,"error":"Unauthorized","message":"Autenticação necessária ou credenciais inválidas","path":"%s"}
                    """.formatted(Instant.now(), request.getRequestURI()));
            response.getWriter().flush();
        };

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cadastro de usuário aberto ao público
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        // Endpoint de autenticação
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/auth").permitAll()
                        // Validação de existência inter-serviços (autenticada por token de serviço X-Service-Token)
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}/exists").permitAll()
                        // Documentação Swagger e OpenAPI abertas
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Todas as demais rotas requerem autenticação (HTTP Basic)
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("""
                                    {"timestamp":"%s","status":403,"error":"Forbidden","message":"%s","path":"%s"}
                                    """.formatted(Instant.now(),
                                    accessDeniedException.getMessage() != null ? accessDeniedException.getMessage() : "Acesso negado",
                                    request.getRequestURI()));
                            response.getWriter().flush();
                        })
                );

        return http.build();
    }
}
