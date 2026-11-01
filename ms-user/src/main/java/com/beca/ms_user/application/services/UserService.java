package com.beca.ms_user.application.services;

import com.beca.ms_user.application.ports.in.CreateUserUseCase;
import com.beca.ms_user.application.ports.in.DeleteUserUseCase;
import com.beca.ms_user.application.ports.in.FindUserUseCase;
import com.beca.ms_user.application.ports.in.UpdateUserUseCase;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.exceptions.UserNotFoundException;
import com.beca.ms_user.domain.models.User;

import java.util.List;

public class UserService implements CreateUserUseCase, FindUserUseCase, UpdateUserUseCase, DeleteUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public UserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User execute(User user) {
        // Duplicate emails/CPFs are not allowed.
        userRepositoryPort.findByEmail(user.getEmail())
                .ifPresent(u -> {
                    throw  new IllegalArgumentException("E-mail já cadastrado no sistema.");
                });
        userRepositoryPort.findByCpf(user.getCpf())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("CPF já cadastrado no sistema.");
                });

        return userRepositoryPort.save(User user);
    }

    @Override
    public User findById(Long id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado. ID: " + id));
    }

    @Override
    public List<User> findAll() {
        return userRepositoryPort.findAll();
    }

    @Override
    public User update(Long id, User updatedUsr) {
        User currentUser = findById(id);
        userRepositoryPort.findByEmail(updatedUsr.getEmail())
                .ifPresent(u -> {
                    if(!u.getId().equals(id)) {
                        throw new IllegalArgumentException("E-mail já cadastrado na base de dados.");
                    }
                });

        currentUser.setName(updatedUsr.getName());
        currentUser.setEmail(updatedUsr.getEmail());
        currentUser.setPhone(updatedUsr.getPhone());

        return userRepositoryPort.save(currentUser);
    }

    @Override
    public void delete(Long id) {
        findById(id);

        userRepositoryPort.deleteById(id);
    }
}
