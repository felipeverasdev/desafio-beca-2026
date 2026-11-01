package com.beca.ms_user.application.ports.out;

import com.beca.ms_user.domain.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByCpf(String cpf);
    List<User> findAll();
    void deleteById(Long id);
}
