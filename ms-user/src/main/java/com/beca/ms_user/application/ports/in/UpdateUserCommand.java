package com.beca.ms_user.application.ports.in;

import java.util.UUID;

public record UpdateUserCommand(
        UUID id,
        String name,
        String phone
) {
    public UpdateUserCommand {
        if(id == null) {
            throw new IllegalArgumentException("ID de usuário é obrigatório");
        }
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if(phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("O número de celular é obrigatório");
        }
    }
}
