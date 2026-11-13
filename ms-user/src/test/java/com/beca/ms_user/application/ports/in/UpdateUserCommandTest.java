package com.beca.ms_user.application.ports.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateUserCommandTest {

    @Test
    @DisplayName("Deve instanciar UpdateUserCommand com sucesso")
    void shouldCreateCommandSuccessfully() {
        UpdateUserCommand command = new UpdateUserCommand("Fernanda Lima", "5511987654321");

        assertEquals("Fernanda Lima", command.name());
        assertEquals("5511987654321", command.phone());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome for nulo, em branco ou menor que 3 caracteres")
    void shouldThrowExceptionWhenNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand(null, "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("   ", "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fe", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o celular for nulo, em branco ou fora do padrão E.164")
    void shouldThrowExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", null));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "   "));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "11987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome exceder 150 caracteres")
    void shouldThrowExceptionWhenNameExceeds150Chars() {
        String longName = "B".repeat(151);

        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand(longName, "5511987654321"));
    }
}
