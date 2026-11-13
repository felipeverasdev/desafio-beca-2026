package com.beca.ms_user.infrastructure.adapters.in.rest;

import com.beca.ms_user.application.ports.in.*;
import com.beca.ms_user.domain.exceptions.InvalidCredentialsException;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.*;
import com.beca.ms_user.infrastructure.adapters.out.security.UserSecurityDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Users",
        description = "Endpoints para gerenciamento do ciclo de vida de usuários")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final ImportUsersCsvUseCase importUsersCsvUseCase;
    private final String internalServiceToken;

    public UserController(CreateUserUseCase createUserUseCase,
                          FindUserUseCase findUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          ImportUsersCsvUseCase importUsersCsvUseCase,
                          @Value("${services.internal.token}") String internalServiceToken) {
        this.createUserUseCase = createUserUseCase;
        this.findUserUseCase = findUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.importUsersCsvUseCase = importUsersCsvUseCase;
        this.internalServiceToken = internalServiceToken;
    }

    @Operation(summary = "Criar um novo usuário",
            description = "Cadastra um novo perfil no banco com validações de email, telefone e CPF")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mails/CPFs/telefones duplicados")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserRequestDTO dto) {
        CreateUserCommand userCommand = new CreateUserCommand(
                dto.name().trim(), dto.email().trim().toLowerCase(), dto.password(), dto.cpf().trim(), dto.phone().trim()
        );
        User created = createUserUseCase.create(userCommand);

        UserResponseDTO response = new UserResponseDTO(created.getId(), created.getName(),
                created.getEmail(), created.getCpf(), created.getPhone(),
                created.getCreatedAt(), created.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Importação em massa de usuários via arquivo CSV",
            description = "Recebe um arquivo CSV com colunas (name,email,password,cpf,phone). Linhas inválidas ou com e-mails duplicados são reportadas individualmente sem abortar o restante da importação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Arquivo processado com resumo de sucessos e falhas"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou não fornecido")
    })
    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportResultDTO> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo CSV não pode estar vazio");
        }

        try {
            CsvImportResult result = importUsersCsvUseCase.execute(file.getInputStream());

            List<CsvRowErrorDTO> errorDtos = result.errors().stream()
                    .map(err -> new CsvRowErrorDTO(err.lineNumber(), err.email(), err.reason()))
                    .toList();

            CsvImportResultDTO response = new CsvImportResultDTO(
                    result.totalRows(),
                    result.successCount(),
                    result.failureCount(),
                    errorDtos
            );

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha ao ler o arquivo enviado: " + e.getMessage());
        }
    }

    @Operation(summary = "Autenticar usuário",
            description = "Valida as credenciais (e-mail e senha) e retorna os dados do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/auth")
    public ResponseEntity<UserResponseDTO> authenticate(@RequestBody @Valid AuthRequestDTO dto) {
        User user = authenticateUserUseCase.authenticate(dto.email().trim().toLowerCase(), dto.password());

        UserResponseDTO response = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar usuário por identificador (ID)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: o usuário autenticado não pode consultar dados de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado no sistema")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal UserSecurityDetails authenticatedUser) {
        validateOwnership(authenticatedUser, id);

        User user = findUserUseCase.findById(id);

        UserResponseDTO response = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar existência de usuário ativo (Inter-serviços)",
            description = "Endpoint para validação síncrona inter-serviços. Requer credencial de serviço no cabeçalho X-Service-Token. " +
                    "Retorna 200 se o usuário existe e está ativo, 404 se não localizado ou inativo, ou 401 se a credencial for inválida ou ausente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário existe e está ativo"),
            @ApiResponse(responseCode = "401", description = "Credencial de serviço ausente ou inválida"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado ou inativo")
    })
    @GetMapping("/{id}/exists")
    public ResponseEntity<Void> checkExists(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Service-Token", required = false) String serviceToken) {
        if (serviceToken == null || !serviceToken.equals(internalServiceToken)) {
            throw new InvalidCredentialsException("Credencial de serviço inválida ou ausente no cabeçalho X-Service-Token");
        }
        findUserUseCase.findById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Buscar usuário por e-mail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: o usuário autenticado só pode consultar o próprio e-mail"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado no sistema")
    })
    @GetMapping("/by-email")
    public ResponseEntity<UserResponseDTO> findByEmail(@RequestParam String email,
                                                       @AuthenticationPrincipal UserSecurityDetails authenticatedUser) {
        if (authenticatedUser == null || !authenticatedUser.getUsername().equalsIgnoreCase(email != null ? email.trim() : "")) {
            throw new AccessDeniedException("Você não tem permissão para consultar os dados de outro usuário");
        }

        User user = findUserUseCase.findByEmail(email.trim().toLowerCase());

        UserResponseDTO response = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Atualizar dados de um usuário existente",
            description = "Permite a atualização de nome e celular do próprio usuário autenticado. O e-mail é imutável e não pode ser alterado por este endpoint.")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id,
                                                  @RequestBody @Valid UserUpdateDTO dto,
                                                  @AuthenticationPrincipal UserSecurityDetails authenticatedUser) {
        validateOwnership(authenticatedUser, id);

        UpdateUserCommand updated = new UpdateUserCommand(
                dto.name().trim(), dto.phone().trim()
        );
        User saved = updateUserUseCase.update(id, updated);

        UserResponseDTO response = new UserResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getCpf(),
                saved.getPhone(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remover usuário por identificador (ID)",
    description = "Deve desativar o usuário, mas manter os seus dados no banco de dados.")
    @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal UserSecurityDetails authenticatedUser) {
        validateOwnership(authenticatedUser, id);

        deleteUserUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar usuários excluídos do sistema",
            description = "Retorna histórico de usuários desativados (soft-deleted). Não expõe dados sensíveis (sem CPF e sem telefone). Requer autenticação HTTP Basic.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários deletados retornada com sucesso")
    })
    @GetMapping("/deleted")
    public ResponseEntity<Page<DeletedUserResponseDTO>> findDeleted(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<User> deletedUsers = findUserUseCase.findAllDeleted(pageable);
        Page<DeletedUserResponseDTO> response = deletedUsers.map(user -> new DeletedUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDeletedAt()
        ));

        return ResponseEntity.ok(response);
    }

    private void validateOwnership(UserSecurityDetails authenticatedUser, UUID targetId) {
        if (authenticatedUser == null || !authenticatedUser.getId().equals(targetId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar ou alterar os dados de outro usuário");
        }
    }
}
