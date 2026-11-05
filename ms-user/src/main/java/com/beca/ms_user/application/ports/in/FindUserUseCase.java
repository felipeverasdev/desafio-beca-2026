package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FindUserUseCase {
    User findById(UUID id);
    User findByEmail(String email);
    Page<User> findAll(Pageable pageable);
}
