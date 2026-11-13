package com.beca.ms_transaction.domain.models;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}

