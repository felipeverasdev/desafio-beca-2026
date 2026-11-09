package com.beca.ms_user.application.ports.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateUserCommandTest {

    @Test
    @DisplayName("Deve instanciar CreateUserCommand com sucesso e converter email para minúsculo")
    void shouldCreateCommandSuccessfully() {
        CreateUserCommand command = new CreateUserCommand(
                "Carlos Alberto",
                "CARLOS@EMAIL.COM",
                "senhaForte123",
                "12345678901",
                "5511987654321"
        );

        assertEquals("Carlos Alberto", command.name());
        assertEquals("carlos@email.com", command.email());
        assertEquals("senhaForte123", command.password());
        assertEquals("12345678901", command.cpf());
        assertEquals("5511987654321", command.phone());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome for nulo, em branco ou menor que 3 caracteres")
    void shouldThrowExceptionWhenNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand(null, "email@email.com", "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("   ", "email@email.com", "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Ab", "email@email.com", "senha123", "12345678901", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o e-mail for nulo, em branco ou com formato inválido")
    void shouldThrowExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", null, "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "   ", "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "invalido", "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "invalido@semdominio", "senha123", "12345678901", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando a senha for nula, em branco ou menor que 6 caracteres")
    void shouldThrowExceptionWhenPasswordIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", null, "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "   ", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "12345", "12345678901", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o CPF for nulo, em branco ou diferente de 11 dígitos")
    void shouldThrowExceptionWhenCpfIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", null, "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "   ", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "123456789", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "123456789012", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "1234567890A", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o celular for nulo, em branco ou fora do padrão E.164")
    void shouldThrowExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", null));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", "   "));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", "11987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", "5511887654321"));
    }
}

