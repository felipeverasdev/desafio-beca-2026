package com.beca.ms_transaction.application.ports.out;

import com.beca.ms_transaction.domain.models.AuthenticatedUser;

import java.util.Optional;

public interface UserAuthenticationPort {
    Optional<AuthenticatedUser> authenticate(String email, String rawPassword);
}

