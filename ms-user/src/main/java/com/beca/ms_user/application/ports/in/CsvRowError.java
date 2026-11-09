package com.beca.ms_user.application.ports.in;

public record CsvRowError(
        int lineNumber,
        String email,
        String reason
) {
}

