package com.beca.ms_user.application.services;

import com.beca.ms_user.application.ports.in.*;
import com.beca.ms_user.application.ports.out.PasswordEncoderPort;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.exceptions.InvalidCredentialsException;
import com.beca.ms_user.domain.exceptions.ResourceAlreadyExistsException;
import com.beca.ms_user.domain.exceptions.UserNotFoundException;
import com.beca.ms_user.domain.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserService implements CreateUserUseCase, FindUserUseCase, UpdateUserUseCase, DeleteUserUseCase, AuthenticateUserUseCase, ImportUsersCsvUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public UserService(UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public User create(CreateUserCommand command) {
        // Duplicate emails/CPFs/phones are not allowed.
        userRepositoryPort.findByEmail(command.email())
                .ifPresent(u -> {
                    throw new ResourceAlreadyExistsException("E-mail já cadastrado no sistema.");
                });
        userRepositoryPort.findByCpf(command.cpf())
                .ifPresent(u -> {
                    throw new ResourceAlreadyExistsException("CPF já cadastrado no sistema.");
                });
        userRepositoryPort.findByPhone(command.phone())
                .ifPresent(u -> {
                    throw new ResourceAlreadyExistsException("Telefone já cadastrado no sistema.");
                });

        String encodedPassword = passwordEncoderPort.encode(command.password());
        User created = User.create(command.name(), command.email(), encodedPassword, command.cpf(), command.phone());
        return userRepositoryPort.save(created);
    }

    @Override
    public User authenticate(String email, String rawPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (!passwordEncoderPort.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        return user;
    }

    @Override
    public User findById(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado. ID: " + id));
    }

    @Override
    public User findByEmail(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        return userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado. E-mail: " + email));
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    public Page<User> findAllDeleted(Pageable pageable) {
        return userRepositoryPort.findAllDeleted(pageable);
    }

    @Override
    @Transactional
    public User update(UUID id, UpdateUserCommand command) {
        User currentUser = findById(id);

        if (!currentUser.getPhone().equals(command.phone())) {
            userRepositoryPort.findByPhone(command.phone())
                    .ifPresent(existing -> {
                        if (existing.getId() == null || !existing.getId().equals(id)) {
                            throw new ResourceAlreadyExistsException("Telefone já cadastrado no sistema.");
                        }
                    });
        }

        currentUser.updatePersonalData(command.name(), command.phone());

        return userRepositoryPort.save(currentUser);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = findById(id);

        // Soft delete
        user.delete();

        userRepositoryPort.save(user);
    }

    @Override
    public CsvImportResult execute(InputStream inputStream) {
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;
        List<CsvRowError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }

                if (isHeader) {
                    isHeader = false;
                    String lower = trimmedLine.toLowerCase();
                    if (lower.contains("name") || lower.contains("nome") || lower.contains("email")) {
                        continue;
                    }
                }

                totalRows++;
                String delimiter = trimmedLine.contains(";") ? ";" : ",";
                String[] columns = trimmedLine.split(delimiter, -1);

                if (columns.length < 5) {
                    failureCount++;
                    errors.add(new CsvRowError(lineNumber, "", "Formato inválido. Esperadas 5 colunas: name,email,password,cpf,phone"));
                    continue;
                }

                String name = columns[0].trim();
                String email = columns[1].trim().toLowerCase();
                String password = columns[2].trim();
                String cpf = columns[3].trim();
                String phone = columns[4].trim();

                try {
                    CreateUserCommand command = new CreateUserCommand(name, email, password, cpf, phone);
                    this.create(command);
                    successCount++;
                } catch (IllegalArgumentException | ResourceAlreadyExistsException ex) {
                    failureCount++;
                    errors.add(new CsvRowError(lineNumber, email, ex.getMessage()));
                } catch (Exception ex) {
                    failureCount++;
                    errors.add(new CsvRowError(lineNumber, email, "Erro inesperado: " + ex.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao processar leitura do arquivo CSV: " + e.getMessage());
        }

        return new CsvImportResult(totalRows, successCount, failureCount, errors);
    }

}
