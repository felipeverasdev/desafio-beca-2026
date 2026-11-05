package com.beca.ms_user.application.ports.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateUserCommandTest {

    @Test
    @DisplayName("Deve instanciar CreateUserCommand com sucesso quando os dados forem válidos")
    void shouldCreateCommandSuccessfully() {
        CreateUserCommand command = new CreateUserCommand(
                "Carlos Alberto",
                "carlos@email.com",
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
    @DisplayName("Deve lançar exceção quando o nome for nulo ou em branco")
    void shouldThrowExceptionWhenNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand(null, "email@email.com", "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("   ", "email@email.com", "senha123", "12345678901", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o e-mail for nulo ou em branco")
    void shouldThrowExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", null, "senha123", "12345678901", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "   ", "senha123", "12345678901", "5511987654321"));
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
    @DisplayName("Deve lançar exceção quando o CPF for nulo ou em branco")
    void shouldThrowExceptionWhenCpfIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", null, "5511987654321"));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "   ", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o celular for nulo ou em branco")
    void shouldThrowExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", null));
        assertThrows(IllegalArgumentException.class, () ->
                new CreateUserCommand("Carlos", "email@email.com", "senha123", "12345678901", "   "));
    }
}

