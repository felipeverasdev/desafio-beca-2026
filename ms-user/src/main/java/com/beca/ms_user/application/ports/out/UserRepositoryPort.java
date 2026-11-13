package com.beca.ms_user.application.ports.out;

import com.beca.ms_user.application.ports.in.CreateUserCommand;
import com.beca.ms_user.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByCpf(String cpf);
    Optional<User> findByPhone(String phone);
    Page<User> findAll(Pageable pageable);
    Page<User> findAllDeleted(Pageable pageable);
}
