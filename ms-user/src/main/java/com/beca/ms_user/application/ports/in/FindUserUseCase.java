package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

import java.util.List;
import java.util.UUID;

public interface FindUserUseCase {
    User findById(UUID id);
    List<User> findAll();
}
