package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

public interface UpdateUserUseCase {
    User update(Long id, User user);
}
