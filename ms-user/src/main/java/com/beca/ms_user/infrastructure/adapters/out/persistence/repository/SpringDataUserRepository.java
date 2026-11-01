package com.beca.ms_user.infrastructure.adapters.out.persistence.repository;

import com.beca.ms_user.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByCpf(String cpf);
}
