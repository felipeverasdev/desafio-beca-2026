package com.beca.ms_user.infrastructure.adapters.out.persistence;

import com.beca.ms_user.application.ports.in.CreateUserCommand;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.models.User;
import com.beca.ms_user.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.beca.ms_user.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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
    public Optional<User> findById(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailAndDeletedAtIsNull(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        return repository.findByCpfAndDeletedAtIsNull(cpf).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return repository.findByPhoneAndDeletedAtIsNull(phone).map(this::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return repository.findAllByDeletedAtIsNull(pageable).map(this::toDomain);
    }

    @Override
    public Page<User> findAllDeleted(Pageable pageable) {
        return repository.findAllByDeletedAtIsNotNull(pageable).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(),
                entity.getPassword(), entity.getCpf(), entity.getPhone(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }

    private UserJpaEntity toEntity(User domain) {
        return new UserJpaEntity(domain.getId(), domain.getName(), domain.getEmail(),
                domain.getPassword(), domain.getCpf(), domain.getPhone(),
                domain.getCreatedAt(), domain.getUpdatedAt(), domain.getDeletedAt());
    }
}
