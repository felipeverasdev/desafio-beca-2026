package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

import java.util.UUID;

public record UserSummaryDTO(
        UUID id,
        String name,
        String email
) {
}
