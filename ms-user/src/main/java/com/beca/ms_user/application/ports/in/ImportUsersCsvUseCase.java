package com.beca.ms_user.application.ports.in;

import java.io.InputStream;

public interface ImportUsersCsvUseCase {
    CsvImportResult execute(InputStream inputStream);
}

