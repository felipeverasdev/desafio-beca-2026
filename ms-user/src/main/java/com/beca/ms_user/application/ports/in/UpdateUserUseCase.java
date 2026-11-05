package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

import java.util.UUID;

public interface UpdateUserUseCase {
    User update(UUID id, UpdateUserCommand userCommand);
}
