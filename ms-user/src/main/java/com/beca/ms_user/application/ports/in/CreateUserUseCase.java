package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

public interface CreateUserUseCase {
    User execute(User user);
}
