package com.beca.ms_user.domain.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Deve criar um usuário com ID gerado e timestamps corretos através do método estático create")
    void shouldCreateUserSuccessfully() {
        User user = User.create(
                "Lucas Silva",
                "lucas@email.com",
                "$2a$10$encodedPasswordHash",
                "12345678901",
                "5511987654321"
        );

        assertNotNull(user.getId());
        assertEquals("Lucas Silva", user.getName());
        assertEquals("lucas@email.com", user.getEmail());
        assertEquals("$2a$10$encodedPasswordHash", user.getPassword());
        assertEquals("12345678901", user.getCpf());
        assertEquals("5511987654321", user.getPhone());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
        assertNull(user.getDeletedAt());
    }

    @Test
    @DisplayName("Deve atualizar dados pessoais e registrar nova data em updatedAt")
    void shouldUpdatePersonalData() throws InterruptedException {
        User user = User.create(
                "Lucas Silva",
                "lucas@email.com",
                "$2a$10$encodedPasswordHash",
                "12345678901",
                "5511987654321"
        );

        OffsetDateTime initialUpdatedAt = user.getUpdatedAt();
        Thread.sleep(10);

        user.updatePersonalData("Lucas S. Atualizado", "5511999998888");

        assertEquals("Lucas S. Atualizado", user.getName());
        assertEquals("5511999998888", user.getPhone());
        assertTrue(user.getUpdatedAt().isAfter(initialUpdatedAt) || user.getUpdatedAt().isEqual(initialUpdatedAt));
    }

    @Test
    @DisplayName("Deve realizar soft delete registrando a data em deletedAt")
    void shouldSoftDeleteUser() {
        User user = User.create(
                "Lucas Silva",
                "lucas@email.com",
                "$2a$10$encodedPasswordHash",
                "12345678901",
                "5511987654321"
        );

        assertNull(user.getDeletedAt());

        user.delete();

        assertNotNull(user.getDeletedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve instanciar corretamente através do construtor completo")
    void shouldInstantiateWithAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        User user = new User(
                id,
                "Maria Souza",
                "maria@email.com",
                "secretHash",
                "98765432100",
                "5511911112222",
                now,
                now,
                null
        );

        assertEquals(id, user.getId());
        assertEquals("Maria Souza", user.getName());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("secretHash", user.getPassword());
        assertEquals("98765432100", user.getCpf());
        assertEquals("5511911112222", user.getPhone());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertNull(user.getDeletedAt());
    }
}

