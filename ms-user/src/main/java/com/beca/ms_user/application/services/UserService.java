package com.beca.ms_user.application.services;

import com.beca.ms_user.application.ports.in.*;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.exceptions.ResourceAlreadyExistsException;
import com.beca.ms_user.domain.exceptions.UserNotFoundException;
import com.beca.ms_user.domain.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public class UserService implements CreateUserUseCase, FindUserUseCase, UpdateUserUseCase, DeleteUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public UserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    @Transactional
    public User create(CreateUserCommand command) {
        // Duplicate emails/CPFs are not allowed.
        userRepositoryPort.findByEmail(command.email())
                .ifPresent(u -> {
                    throw  new ResourceAlreadyExistsException("E-mail já cadastrado no sistema.");
                });
        userRepositoryPort.findByCpf(command.cpf())
                .ifPresent(u -> {
                    throw new ResourceAlreadyExistsException("CPF já cadastrado no sistema.");
                });

        User created = User.Create(command.name(), command.email(), command.cpf(), command.phone());
        return userRepositoryPort.save(created);
    }

    @Override
    public User findById(UUID id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado. ID: " + id));
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional
    public User update(UUID id, UpdateUserCommand command) {
        User currentUser = findById(id);

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

}
