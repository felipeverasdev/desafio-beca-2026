package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.models.ExpenseAnalysis;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyzeExpensesUseCase {
    ExpenseAnalysis execute(UUID userId, LocalDate startDate, LocalDate endDate);
}

