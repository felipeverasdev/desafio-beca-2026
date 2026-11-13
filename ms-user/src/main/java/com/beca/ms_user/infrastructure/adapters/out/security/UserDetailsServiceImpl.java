package com.beca.ms_user.infrastructure.adapters.out.security;

import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.domain.models.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    public UserDetailsServiceImpl(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedEmail = username != null ? username.trim().toLowerCase() : "";

        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + username));

        if (user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("Usuário inativo ou excluído do sistema: " + username);
        }

        return new UserSecurityDetails(user);
    }
}

