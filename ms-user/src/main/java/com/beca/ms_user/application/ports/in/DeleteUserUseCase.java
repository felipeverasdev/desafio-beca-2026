package com.beca.ms_user.application.ports.in;

import java.util.UUID;

public interface DeleteUserUseCase {
    void delete(UUID id);
}
