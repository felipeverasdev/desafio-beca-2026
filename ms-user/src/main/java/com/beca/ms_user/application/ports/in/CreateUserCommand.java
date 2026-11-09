package com.beca.ms_user.application.ports.in;

public record CreateUserCommand(
        String name,
        String email,
        String password,
        String cpf,
        String phone
) {
    public CreateUserCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        name = name.trim();
        if (name.length() < 3) {
            throw new IllegalArgumentException("O nome deve conter pelo menos 3 caracteres");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        email = email.trim().toLowerCase();
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Formato de e-mail inválido");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 6 caracteres");
        }
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("O CPF é obrigatório");
        }
        cpf = cpf.trim();
        if (!cpf.matches("^\\d{11}$")) {
            throw new IllegalArgumentException("O CPF deve conter 11 dígitos");
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
