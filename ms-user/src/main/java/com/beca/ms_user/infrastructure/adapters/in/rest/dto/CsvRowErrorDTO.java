package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

public record CsvRowErrorDTO(
        int lineNumber,
        String email,
        String reason
) {
}

