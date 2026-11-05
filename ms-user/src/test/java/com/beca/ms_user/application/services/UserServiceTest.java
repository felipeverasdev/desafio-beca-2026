package com.beca.ms_user.application.services;

import com.beca.ms_user.application.ports.in.CreateUserCommand;
import com.beca.ms_user.application.ports.in.CsvImportResult;
import com.beca.ms_user.application.ports.in.UpdateUserCommand;
import com.beca.ms_user.application.ports.out.PasswordEncoderPort;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.exceptions.InvalidCredentialsException;
import com.beca.ms_user.domain.exceptions.ResourceAlreadyExistsException;
import com.beca.ms_user.domain.exceptions.UserNotFoundException;
import com.beca.ms_user.domain.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private CreateUserCommand createCommand;

    @BeforeEach
    void setUp() {
        sampleUser = User.create(
                "João da Silva",
                "joao@email.com",
                "$2a$10$encodedHash",
                "12345678901",
                "5511987654321"
        );

        createCommand = new CreateUserCommand(
                "João da Silva",
                "joao@email.com",
                "minhaSenha123",
                "12345678901",
                "5511987654321"
        );
    }

    @Test
    @DisplayName("Deve cadastrar usuário com sucesso quando e-mail e CPF forem inéditos")
    void shouldCreateUserSuccessfully() {
        when(userRepositoryPort.findByEmail(createCommand.email())).thenReturn(Optional.empty());
        when(userRepositoryPort.findByCpf(createCommand.cpf())).thenReturn(Optional.empty());
        when(passwordEncoderPort.encode("minhaSenha123")).thenReturn("$2a$10$encodedHash");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(createCommand);

        assertNotNull(created);
        assertEquals(createCommand.name(), created.getName());
        assertEquals(createCommand.email(), created.getEmail());
        assertEquals("$2a$10$encodedHash", created.getPassword());
        verify(passwordEncoderPort).encode("minhaSenha123");
        verify(userRepositoryPort).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException quando o e-mail já estiver cadastrado")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepositoryPort.findByEmail(createCommand.email())).thenReturn(Optional.of(sampleUser));

        ResourceAlreadyExistsException ex = assertThrows(ResourceAlreadyExistsException.class, () ->
                userService.create(createCommand));

        assertEquals("E-mail já cadastrado no sistema.", ex.getMessage());
        verify(userRepositoryPort, never()).save(any());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException quando o CPF já estiver cadastrado")
    void shouldThrowExceptionWhenCpfAlreadyExists() {
        when(userRepositoryPort.findByEmail(createCommand.email())).thenReturn(Optional.empty());
        when(userRepositoryPort.findByCpf(createCommand.cpf())).thenReturn(Optional.of(sampleUser));

        ResourceAlreadyExistsException ex = assertThrows(ResourceAlreadyExistsException.class, () ->
                userService.create(createCommand));

        assertEquals("CPF já cadastrado no sistema.", ex.getMessage());
        verify(userRepositoryPort, never()).save(any());
        verify(passwordEncoderPort, never()).encode(anyString());
    }

    @Test
    @DisplayName("Deve buscar usuário por ID existente")
    void shouldFindUserByIdSuccessfully() {
        UUID id = sampleUser.getId();
        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(sampleUser));

        User found = userService.findById(id);

        assertNotNull(found);
        assertEquals(id, found.getId());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException ao buscar por ID inexistente")
    void shouldThrowExceptionWhenUserNotFoundById() {
        UUID id = UUID.randomUUID();
        when(userRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(id));
    }

    @Test
    @DisplayName("Deve buscar usuário por e-mail existente")
    void shouldFindUserByEmailSuccessfully() {
        when(userRepositoryPort.findByEmail("joao@email.com")).thenReturn(Optional.of(sampleUser));

        User found = userService.findByEmail("joao@email.com");

        assertNotNull(found);
        assertEquals("joao@email.com", found.getEmail());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException ao buscar por e-mail inexistente")
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        when(userRepositoryPort.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findByEmail("inexistente@email.com"));
    }

    @Test
    @DisplayName("Deve listar usuários de forma paginada")
    void shouldFindAllUsersPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(sampleUser), pageable, 1);
        when(userRepositoryPort.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("João da Silva", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Deve atualizar dados pessoais de um usuário existente")
    void shouldUpdateUserSuccessfully() {
        UUID id = sampleUser.getId();
        UpdateUserCommand updateCommand = new UpdateUserCommand("João Atualizado", "5511900001111");

        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(sampleUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.update(id, updateCommand);

        assertEquals("João Atualizado", updated.getName());
        assertEquals("5511900001111", updated.getPhone());
        verify(userRepositoryPort).save(sampleUser);
    }

    @Test
    @DisplayName("Deve realizar soft delete em usuário existente")
    void shouldDeleteUserSuccessfully() {
        UUID id = sampleUser.getId();
        when(userRepositoryPort.findById(id)).thenReturn(Optional.of(sampleUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.delete(id);

        assertNotNull(sampleUser.getDeletedAt());
        verify(userRepositoryPort).save(sampleUser);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando e-mail existir e a senha conferir")
    void shouldAuthenticateSuccessfully() {
        when(userRepositoryPort.findByEmail("joao@email.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoderPort.matches("minhaSenha123", sampleUser.getPassword())).thenReturn(true);

        User authenticated = userService.authenticate("joao@email.com", "minhaSenha123");

        assertNotNull(authenticated);
        assertEquals(sampleUser.getId(), authenticated.getId());
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando o e-mail não existir na autenticação")
    void shouldThrowExceptionWhenEmailNotFoundOnAuth() {
        when(userRepositoryPort.findByEmail("desconhecido@email.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                userService.authenticate("desconhecido@email.com", "senha"));
    }

    @Test
    @DisplayName("Deve lançar InvalidCredentialsException quando a senha estiver incorreta")
    void shouldThrowExceptionWhenPasswordDoesNotMatchOnAuth() {
        when(userRepositoryPort.findByEmail("joao@email.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoderPort.matches("senhaErrada", sampleUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                userService.authenticate("joao@email.com", "senhaErrada"));
    }

    @Test
    @DisplayName("Deve importar CSV com sucesso quando todas as linhas forem válidas")
    void shouldImportCsvSuccessfullyWhenAllRowsAreValid() {
        String csvData = """
                name,email,password,cpf,phone
                Maria Silva,maria@email.com,senhaForte123,12345678901,5511987654321
                Carlos Souza,carlos@email.com,senhaForte123,98765432100,5511988887777
                """;
        ByteArrayInputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepositoryPort.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoderPort.encode(anyString())).thenReturn("hashedPass");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvImportResult result = userService.execute(is);

        assertEquals(2, result.totalRows());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        assertTrue(result.errors().isEmpty());
        verify(userRepositoryPort, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve reportar erro na linha com dado inválido ou duplicado sem abortar as demais linhas")
    void shouldReportRowErrorWithoutAbortingRemainingRows() {
        String csvData = """
                name,email,password,cpf,phone
                Maria Silva,maria@email.com,senhaForte123,12345678901,5511987654321
                Carlos Invalido,carlos@email.com,123,98765432100,5511988887777
                Ana Paula,ana@email.com,senhaForte123,55544433322,5521999998888
                """;
        ByteArrayInputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepositoryPort.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoderPort.encode(anyString())).thenReturn("hashedPass");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvImportResult result = userService.execute(is);

        assertEquals(3, result.totalRows());
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(1, result.errors().size());
        assertEquals(3, result.errors().get(0).lineNumber());
        assertEquals("carlos@email.com", result.errors().get(0).email());
        assertTrue(result.errors().get(0).reason().contains("senha"));
        verify(userRepositoryPort, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve suportar separador ponto e vírgula no CSV")
    void shouldHandleSemicolonSeparatorInCsv() {
        String csvData = """
                name;email;password;cpf;phone
                Maria Silva;maria@email.com;senhaForte123;12345678901;5511987654321
                """;
        ByteArrayInputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        when(userRepositoryPort.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepositoryPort.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoderPort.encode(anyString())).thenReturn("hashedPass");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvImportResult result = userService.execute(is);

        assertEquals(1, result.totalRows());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }
}
