package com.beca.ms_user.application.ports.in;

public record UpdateUserCommand(
        String name,
        String phone
) {
    public UpdateUserCommand {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if(phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("O número de celular é obrigatório");
        }
    }
}

