package com.beca.ms_user.application.ports.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateUserCommandTest {

    @Test
    @DisplayName("Deve instanciar UpdateUserCommand com sucesso e converter e-mail para minúsculo")
    void shouldCreateCommandSuccessfully() {
        UpdateUserCommand command = new UpdateUserCommand("Fernanda Lima", "FERNANDA@EMAIL.COM", "5511987654321");

        assertEquals("Fernanda Lima", command.name());
        assertEquals("fernanda@email.com", command.email());
        assertEquals("5511987654321", command.phone());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome for nulo, em branco ou menor que 3 caracteres")
    void shouldThrowExceptionWhenNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand(null, "fernanda@email.com", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("   ", "fernanda@email.com", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fe", "fernanda@email.com", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o e-mail for nulo, em branco ou com formato inválido")
    void shouldThrowExceptionWhenEmailIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", null, "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "   ", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "email_invalido", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o celular for nulo, em branco ou fora do padrão E.164")
    void shouldThrowExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "fernanda@email.com", null));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "fernanda@email.com", "   "));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "fernanda@email.com", "11987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome ou email exceder 150 caracteres")
    void shouldThrowExceptionWhenNameOrEmailExceeds150Chars() {
        String longName = "B".repeat(151);
        String longEmail = "b".repeat(140) + "@domain.com";

        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand(longName, "fernanda@email.com", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", longEmail, "5511987654321"));
    }
}

