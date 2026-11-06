package com.beca.ms_user.application.ports.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateUserCommandTest {

    @Test
    @DisplayName("Deve instanciar UpdateUserCommand com sucesso quando os dados forem válidos")
    void shouldCreateCommandSuccessfully() {
        UpdateUserCommand command = new UpdateUserCommand("Fernanda Lima", "5511987654321");

        assertEquals("Fernanda Lima", command.name());
        assertEquals("5511987654321", command.phone());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o nome for nulo ou em branco")
    void shouldThrowExceptionWhenNameIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand(null, "5511987654321"));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("   ", "5511987654321"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando o celular for nulo ou em branco")
    void shouldThrowExceptionWhenPhoneIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", null));
        assertThrows(IllegalArgumentException.class, () -> new UpdateUserCommand("Fernanda", "   "));
    }
}

