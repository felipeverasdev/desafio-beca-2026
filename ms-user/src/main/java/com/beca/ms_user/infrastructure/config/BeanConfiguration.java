package com.beca.ms_user.infrastructure.config;

import com.beca.ms_user.application.ports.out.PasswordEncoderPort;
import com.beca.ms_user.application.ports.out.UserRepositoryPort;
import com.beca.ms_user.application.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
    @Bean
    public UserService userService(UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        return new UserService(userRepositoryPort, passwordEncoderPort);
    }
}
