package com.beca.ms_user.application.ports.in;

import java.util.List;

public record CsvImportResult(
        int totalRows,
        int successCount,
        int failureCount,
        List<CsvRowError> errors
) {
}

