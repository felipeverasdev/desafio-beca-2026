package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    public UserResponseDTO(UUID id, String name, String email, String cpf, String phone, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, name, email, cpf, phone, createdAt, updatedAt, null);
    }
}
