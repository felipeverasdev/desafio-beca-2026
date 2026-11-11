package com.beca.ms_user.infrastructure.adapters.in.rest;

import com.beca.ms_user.application.ports.in.CreateUserCommand;
import com.beca.ms_user.application.ports.in.CreateUserUseCase;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class UserControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private CreateUserUseCase createUserUseCase;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        springDataUserRepository.deleteAll();

        userA = createUserUseCase.create(new CreateUserCommand(
                "Usuario A",
                "usera@email.com",
                "senha123",
                "12345678901",
                "5511987654321"
        ));

        userB = createUserUseCase.create(new CreateUserCommand(
                "Usuario B",
                "userb@email.com",
                "senha123",
                "98765432100",
                "5511912345678"
        ));
    }

    @Test
    @DisplayName("GET /api/v1/users/deleted deve retornar 401 Unauthorized sem credenciais")
    void shouldReturn401OnDeletedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/deleted"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/users/deleted deve retornar 200 OK quando autenticado")
    void shouldReturn200OnDeletedWithAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/deleted")
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} deve retornar 401 Unauthorized sem credenciais")
    void shouldReturn401OnFindByIdWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", userA.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} deve retornar 403 Forbidden quando usuário autenticado tentar consultar outro")
    void shouldReturn403WhenUserConsultsAnotherById() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", userB.getId())
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} deve retornar 200 OK quando usuário autenticado consultar a si mesmo")
    void shouldReturn200WhenUserConsultsSelfById() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", userA.getId())
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userA.getId().toString()))
                .andExpect(jsonPath("$.email").value("usera@email.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/by-email deve retornar 401 Unauthorized sem credenciais")
    void shouldReturn401OnFindByEmailWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "usera@email.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/users/by-email deve retornar 403 Forbidden quando usuário consultar email de outro")
    void shouldReturn403WhenUserConsultsAnotherByEmail() throws Exception {
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "userb@email.com")
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/users/by-email deve retornar 200 OK quando usuário consultar seu próprio email")
    void shouldReturn200WhenUserConsultsSelfByEmail() throws Exception {
        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "usera@email.com")
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userA.getId().toString()))
                .andExpect(jsonPath("$.email").value("usera@email.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} deve retornar 403 Forbidden quando usuário tentar alterar dados de outro")
    void shouldReturn403WhenUserUpdatesAnother() throws Exception {
        String updatePayload = """
                {
                    "name": "Nome Alterado",
                    "email": "usera@email.com",
                    "phone": "5511987654321"
                }
                """;

        mockMvc.perform(put("/api/v1/users/{id}", userB.getId())
                        .with(httpBasic("usera@email.com", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} deve retornar 403 Forbidden quando usuário tentar excluir outro")
    void shouldReturn403WhenUserDeletesAnother() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userB.getId())
                        .with(httpBasic("usera@email.com", "senha123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/exists deve ser público (200 para usuário ativo)")
    void shouldReturn200OnExistsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}/exists", userA.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/users deve validar limite de 150 caracteres para name e email (400 Bad Request)")
    void shouldReturn400WhenNameOrEmailExceeds150Chars() throws Exception {
        String longName = "A".repeat(151);
        String payload = """
                {
                    "name": "%s",
                    "email": "teste@email.com",
                    "password": "senhaForte123",
                    "cpf": "11122233344",
                    "phone": "5511911112222"
                }
                """.formatted(longName);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].fieldName").value("name"));
    }
}
