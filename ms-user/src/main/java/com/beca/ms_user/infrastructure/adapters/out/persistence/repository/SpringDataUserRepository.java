package com.beca.ms_user.infrastructure.adapters.out.persistence.repository;

import com.beca.ms_user.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmailAndDeletedAtIsNull(String email);
    Optional<UserJpaEntity> findByCpfAndDeletedAtIsNull(String cpf);
    Optional<UserJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
    Page<UserJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
