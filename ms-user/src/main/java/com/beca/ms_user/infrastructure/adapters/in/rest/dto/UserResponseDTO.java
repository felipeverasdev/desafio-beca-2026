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
    public UserResponseDTO {
        cpf = maskCpf(cpf);
    }

    public UserResponseDTO(UUID id, String name, String email, String cpf, String phone, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, name, email, cpf, phone, createdAt, updatedAt, null);
    }

    private static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return cpf;
        }
        String clean = cpf.replaceAll("\\D", "");
        if (clean.length() == 11) {
            return "***." + clean.substring(3, 6) + "." + clean.substring(6, 9) + "-**";
        }
        return "***.***.***-**";
    }
}
