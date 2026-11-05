package com.beca.ms_user.infrastructure.adapters.in.rest;

import com.beca.ms_user.application.ports.in.*;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.AuthRequestDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserRequestDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserResponseDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserSummaryDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public UserController(CreateUserUseCase createUserUseCase,
                          FindUserUseCase findUserUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.findUserUseCase = findUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
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
                dto.name().trim(), dto.email().trim(), dto.password(), dto.cpf().trim(), dto.phone().trim()
        );
        User created = createUserUseCase.create(userCommand);

        UserResponseDTO response = new UserResponseDTO(created.getId(), created.getName(),
                created.getEmail(), created.getCpf(), created.getPhone(),
                created.getCreatedAt(), created.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Autenticar usuário",
            description = "Valida as credenciais (e-mail e senha) e retorna os dados do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/auth")
    public ResponseEntity<UserResponseDTO> authenticate(@RequestBody @Valid AuthRequestDTO dto) {
        User user = authenticateUserUseCase.authenticate(dto.email().trim(), dto.password());

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

    @Operation(summary = "Listar todos os usuários cadastrados (size = 10 por padrão)")
    @GetMapping
    public ResponseEntity<Page<UserSummaryDTO>> findAll(@PageableDefault(size = 10) Pageable pageable) {
        Page<User> usersDomain = findUserUseCase.findAll(pageable);

        Page<UserSummaryDTO> response = usersDomain.map(user -> new UserSummaryDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                ));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar usuário por identificador (ID)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado no sistema")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
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

    @Operation(summary = "Buscar usuário por e-mail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado no sistema")
    })
    @GetMapping("/by-email")
    public ResponseEntity<UserResponseDTO> findByEmail(@RequestParam String email) {
        User user = findUserUseCase.findByEmail(email.trim());

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

    @Operation(summary = "Atualizar dados de um usuário existente")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id,
                                       @RequestBody @Valid UserUpdateDTO dto) {
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
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUserUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
