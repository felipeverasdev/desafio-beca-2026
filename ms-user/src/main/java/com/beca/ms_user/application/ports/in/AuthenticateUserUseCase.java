package com.beca.ms_user.application.ports.in;

import com.beca.ms_user.domain.models.User;

public interface AuthenticateUserUseCase {
    User authenticate(String email, String rawPassword);
}

