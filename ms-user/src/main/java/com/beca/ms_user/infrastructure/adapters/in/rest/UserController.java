package com.beca.ms_user.infrastructure.adapters.in.rest;

import com.beca.ms_user.application.ports.in.CreateUserUseCase;
import com.beca.ms_user.application.ports.in.DeleteUserUseCase;
import com.beca.ms_user.application.ports.in.FindUserUseCase;
import com.beca.ms_user.application.ports.in.UpdateUserUseCase;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserRequestDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserResponseDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserSummaryDTO;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users",
        description = "Endpoints para gerenciamento do ciclo de vida de usuários")
@RestController
@RequestMapping("/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase, FindUserUseCase findUserUseCase, UpdateUserUseCase updateUserUseCase, DeleteUserUseCase deleteUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.findUserUseCase = findUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @Operation(summary = "Criar um novo usuário",
            description = "Cadastra um novo perfil no banco com validações de email, telefone e CPF")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mails/CPFs/telefones duplicados")
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@RequestBody @Valid UserRequestDTO dto) {
        User userDomain = User.prepareCreate(dto.name(), dto.email(), dto.cpf(), dto.phone());
        User created = createUserUseCase.execute(userDomain);

        UserResponseDTO response = new UserResponseDTO(created.getId(), created.getName(),
                created.getEmail(), created.getCpf(), created.getPhone(),
                created.getCreatedAt(), created.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar todos os usuários cadastrados")
    @GetMapping
    public ResponseEntity<List<UserSummaryDTO>> findAll() {
        List<User> usersDomain = findUserUseCase.findAll();

        List<UserSummaryDTO> response = usersDomain.stream()
                .map(user -> new UserSummaryDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                )).toList();

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

    @Operation(summary = "Atualizar dados de um usuário existente")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(@PathVariable UUID id,
                                       @RequestBody @Valid UserUpdateDTO dto) {
        User updated = User.prepareUpdate(id, dto.name(), dto.phone());
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
