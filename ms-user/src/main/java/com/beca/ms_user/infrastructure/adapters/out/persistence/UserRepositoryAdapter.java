package com.beca.ms_user.infrastructure.adapters.out.persistence;

import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.beca.ms_user.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository repository;

    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = repository.save(entity);

        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public User findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public User findByCpf(String cpf) {
        return repository.findByCpf(cpf).map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getCpf(), entity.getPhone());
    }

    private UserJpaEntity toEntity(User domain) {
        return new UserJpaEntity(domain.getId(), domain.getName(), domain.getEmail(), domain.getCpf(), domain.getPhone());
    }
}
