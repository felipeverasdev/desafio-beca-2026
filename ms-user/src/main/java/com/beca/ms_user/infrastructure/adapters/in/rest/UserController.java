package com.beca.ms_user.infrastructure.adapters.in.rest;

import com.beca.ms_user.application.ports.in.CreateUserUseCase;
import com.beca.ms_user.application.ports.in.DeleteUserUseCase;
import com.beca.ms_user.application.ports.in.FindUserUseCase;
import com.beca.ms_user.application.ports.in.UpdateUserUseCase;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.in.rest.dto.UserRequestDTO;
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
    public ResponseEntity<User> create(@RequestBody @Valid UserRequestDTO dto) {
        User userDomain = new User(null, dto.name(), dto.email(), dto.cpf(), dto.phone());
        User created = createUserUseCase.execute(userDomain);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Listar todos os usuários cadastrados")
    @GetMapping
    public ResponseEntity<List<User>> findAll() {
        return ResponseEntity.ok(findUserUseCase.findAll());
    }

    @Operation(summary = "Buscar usuário por identificador (ID)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não localizado no sistema")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return ResponseEntity.ok(findUserUseCase.findById(id));
    }

    @Operation(summary = "Atualizar dados de um usuário existente")
    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id,
                                       @RequestBody @Valid UserUpdateDTO dto) {
        User updated = new User(id, dto.name(), dto.email(), null, dto.phone());

        User saved = updateUserUseCase.update(id, updated);

        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Remover usuário por identificador (ID)")
    @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso")
    @DeleteMapping("/{id}")
    public ResponseEntity<void> delete(@PathVariable Long id) {
        deleteUserUseCase.delete(id);

        return ResponseEntity.noContent().build();
    }
}
