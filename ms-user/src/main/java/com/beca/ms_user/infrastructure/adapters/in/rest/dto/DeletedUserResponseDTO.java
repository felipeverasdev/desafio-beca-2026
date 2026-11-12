package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeletedUserResponseDTO(
        UUID id,
        String name,
        String email,
        OffsetDateTime deletedAt
) {
}

