package com.beca.ms_user.infrastructure.adapters.out.security;

import com.beca.ms_user.domain.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSecurityDetailsTest {

    @Test
    @DisplayName("Deve mapear dados do usuário ativo corretamente para UserDetails")
    void shouldMapActiveUserToUserDetails() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Carlos", "carlos@email.com", "hash123", "12345678901", "5511999998888",
                OffsetDateTime.now(), OffsetDateTime.now(), null);

        UserSecurityDetails details = new UserSecurityDetails(user);

        assertEquals(id, details.getId());
        assertEquals("Carlos", details.getName());
        assertEquals("carlos@email.com", details.getUsername());
        assertEquals("hash123", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Deve desabilitar usuário quando deletedAt não for nulo")
    void shouldDisableUserWhenDeleted() {
        User user = new User(UUID.randomUUID(), "Carlos", "carlos@email.com", "hash123", "12345678901", "5511999998888",
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());

        UserSecurityDetails details = new UserSecurityDetails(user);

        assertFalse(details.isEnabled());
        assertFalse(details.isAccountNonExpired());
        assertFalse(details.isAccountNonLocked());
        assertFalse(details.isCredentialsNonExpired());
    }
}

