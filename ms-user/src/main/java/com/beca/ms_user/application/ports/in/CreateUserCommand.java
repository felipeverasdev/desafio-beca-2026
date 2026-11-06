package com.beca.ms_user.application.ports.in;

public record CreateUserCommand(
        String name,
        String email,
        String password,
        String cpf,
        String phone
) {
    public CreateUserCommand {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if(email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        if(password == null || password.isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória");
        }
        if(password.length() < 6) {
            throw new IllegalArgumentException("A senha deve conter no mínimo 6 caracteres");
        }
        if(cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("O CPF é obrigatório");
        }
        if(phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("O número de celular é obrigatório");
        }
    }
}
