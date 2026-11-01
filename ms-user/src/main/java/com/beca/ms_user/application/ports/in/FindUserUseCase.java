package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

import java.util.List;

public interface FindUserUseCase {
    User findById(Long id);
    List<User> findAll();
}
