package com.beca.ms_transaction.infrastructure.adapters.in.rest;

import com.beca.ms_transaction.application.ports.out.AccountAuthorizationPort;
import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.application.ports.out.UserAuthenticationPort;
import com.beca.ms_transaction.application.ports.out.UserValidationPort;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import com.beca.ms_transaction.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TransactionControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthenticationPort userAuthenticationPort;

    @MockitoBean
    private UserValidationPort userValidationPort;

    @MockitoBean
    private AccountAuthorizationPort accountAuthorizationPort;

    @Autowired
    private TransactionRepositoryPort transactionRepositoryPort;

    private UUID userAId;
    private UUID userBId;
    private final String userAEmail = "usera@email.com";
    private final String userBEmail = "userb@email.com";
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        when(userAuthenticationPort.authenticate(userAEmail, password))
                .thenReturn(Optional.of(new AuthenticatedUser(userAId, userAEmail)));

        when(userAuthenticationPort.authenticate(userBEmail, password))
                .thenReturn(Optional.of(new AuthenticatedUser(userBId, userBEmail)));

        when(userAuthenticationPort.authenticate("invalid@email.com", "wrong"))
                .thenReturn(Optional.empty());

        when(userAuthenticationPort.authenticate("offline@email.com", password))
                .thenThrow(new ExternalServiceUnavailableException("Serviço de usuários indisponível"));
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized para todas as rotas de transação sem autenticação")
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .param("userId", userAId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/transactions/balance/{userId}", userAId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/transactions/analysis")
                        .param("userId", userAId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/transactions/export-csv")
                        .param("userId", userAId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "type": "DEPOSIT",
                                  "amount": 100.00,
                                  "category": "OUTROS",
                                  "description": "Depósito"
                                }
                                """.formatted(userAId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Deve retornar 401 com credenciais inválidas")
    void shouldReturn401WithBadCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/balance/{userId}", userAId)
                        .with(httpBasic("invalid@email.com", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Deve retornar 503 quando ms-user estiver offline durante autenticação")
    void shouldReturn503WhenMsUserDownDuringAuth() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/balance/{userId}", userAId)
                        .with(httpBasic("offline@email.com", password)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Serviço de usuários indisponível"));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar consultar saldo de outro usuário")
    void shouldReturn403WhenConsultingOtherUserBalance() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/balance/{userId}", userBId)
                        .with(httpBasic(userAEmail, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Você não tem permissão para acessar ou operar dados de outro usuário"));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar criar transação para outro usuário")
    void shouldReturn403WhenCreatingTransactionForOtherUser() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic(userAEmail, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "type": "DEPOSIT",
                                  "amount": 100.00,
                                  "category": "OUTROS",
                                  "description": "Tentativa indevida"
                                }
                                """.formatted(userBId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Você não tem permissão para acessar ou operar dados de outro usuário"));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar buscar transações de outro usuário")
    void shouldReturn403WhenSearchingOtherUserTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .with(httpBasic(userAEmail, password))
                        .param("userId", userBId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar analisar despesas de outro usuário")
    void shouldReturn403WhenAnalyzingOtherUserExpenses() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/analysis")
                        .with(httpBasic(userAEmail, password))
                        .param("userId", userBId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar exportar CSV de outro usuário")
    void shouldReturn403WhenExportingOtherUserCsv() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/export-csv")
                        .with(httpBasic(userAEmail, password))
                        .param("userId", userBId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado tentar ver transação pertencente a outro usuário por ID")
    void shouldReturn403WhenAccessingOtherUserTransactionById() throws Exception {
        Transaction txB = transactionRepositoryPort.save(Transaction.createApproved(
                userBId,
                TransactionType.PURCHASE,
                new BigDecimal("50.00"),
                TransactionCategory.ALIMENTACAO,
                "Almoço",
                null
        ));

        mockMvc.perform(get("/api/v1/transactions/{id}", txB.getId())
                        .with(httpBasic(userAEmail, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Deve retornar 200 OK quando usuário autenticado consultar seu próprio saldo")
    void shouldReturn200WhenConsultingOwnBalance() throws Exception {
        when(userValidationPort.existsAndActive(userAId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userAId))
                .thenReturn(Optional.of(new AccountBalance(userAId, new BigDecimal("1500.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"))));

        mockMvc.perform(get("/api/v1/transactions/balance/{userId}", userAId)
                        .with(httpBasic(userAEmail, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userAId.toString()))
                .andExpect(jsonPath("$.balance").value(1500.00));
    }

    @Test
    @DisplayName("Deve retornar 201 Created quando usuário autenticado criar transação para si mesmo")
    void shouldReturn201WhenCreatingOwnTransaction() throws Exception {
        when(userValidationPort.existsAndActive(userAId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userAId))
                .thenReturn(Optional.of(new AccountBalance(userAId, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"))));

        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic(userAEmail, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "type": "DEPOSIT",
                                  "amount": 200.00,
                                  "category": "OUTROS",
                                  "description": "Depósito próprio"
                                }
                                """.formatted(userAId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }
}

