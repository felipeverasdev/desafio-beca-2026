package com.beca.ms_user.infrastructure.adapters.out.security;

import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Deve carregar UserDetails com sucesso para usuário ativo")
    void shouldLoadUserDetailsForActiveUser() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Ana", "ana@email.com", "hashBcrypt", "12345678901", "5511999998888",
                OffsetDateTime.now(), OffsetDateTime.now(), null);

        when(userRepositoryPort.findByEmail("ana@email.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("ANA@EMAIL.COM ");

        assertNotNull(details);
        assertEquals("ana@email.com", details.getUsername());
        assertEquals("hashBcrypt", details.getPassword());
        assertTrue(details.isEnabled());
        verify(userRepositoryPort, times(1)).findByEmail("ana@email.com");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não for encontrado")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepositoryPort.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("inexistente@email.com"));
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário estiver com soft delete")
    void shouldThrowExceptionWhenUserIsDeleted() {
        User deletedUser = new User(UUID.randomUUID(), "Ana", "ana@email.com", "hashBcrypt", "12345678901", "5511999998888",
                OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());

        when(userRepositoryPort.findByEmail("ana@email.com")).thenReturn(Optional.of(deletedUser));

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("ana@email.com"));
    }
}

