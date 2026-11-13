package com.beca.ms_user.infrastructure.adapters.in.rest.dto;

import java.util.List;

public record CsvImportResultDTO(
        int totalRows,
        int successCount,
        int failureCount,
        List<CsvRowErrorDTO> errors
) {
}

