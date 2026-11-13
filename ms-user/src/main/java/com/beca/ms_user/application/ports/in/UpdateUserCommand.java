package com.beca.ms_user.application.ports.in;

public record UpdateUserCommand(
        String name,
        String email,
        String phone
) {
    public UpdateUserCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        name = name.trim();
        if (name.length() < 3 || name.length() > 150) {
            throw new IllegalArgumentException("O nome deve conter entre 3 e 150 caracteres");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        email = email.trim().toLowerCase();
        if (email.length() > 150) {
            throw new IllegalArgumentException("O e-mail deve ter no máximo 150 caracteres");
        }
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("O número de celular é obrigatório");
        }
        phone = phone.trim();
        if (!phone.matches("^[1-9]{2}[1-9]{2}9[0-9]{8}$")) {
            throw new IllegalArgumentException("Formato de número de celular inválido. Exemplo correto: 5511912345678");
        }
    }
}

