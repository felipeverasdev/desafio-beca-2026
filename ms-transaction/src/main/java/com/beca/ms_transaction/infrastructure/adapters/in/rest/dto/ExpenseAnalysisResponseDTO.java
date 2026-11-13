package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.domain.models.ExpenseAnalysis;
import com.beca.ms_transaction.domain.models.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseAnalysisResponseDTO(
        UUID userId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalExpenses,
        long totalTransactions,
        List<CategoryExpenseDTO> byCategory,
        List<DailyExpenseDTO> byDay,
        List<MonthlyExpenseDTO> byMonth
) {
    public static ExpenseAnalysisResponseDTO fromDomain(ExpenseAnalysis domain) {
        if (domain == null) {
            return null;
        }

        List<CategoryExpenseDTO> categories = domain.byCategory().stream()
                .map(c -> new CategoryExpenseDTO(c.category(), c.totalAmount(), c.percentage(), c.transactionCount()))
                .toList();

        List<DailyExpenseDTO> days = domain.byDay().stream()
                .map(d -> new DailyExpenseDTO(d.date(), d.totalAmount(), d.transactionCount()))
                .toList();

        List<MonthlyExpenseDTO> months = domain.byMonth().stream()
                .map(m -> new MonthlyExpenseDTO(m.yearMonth(), m.totalAmount(), m.transactionCount()))
                .toList();

        return new ExpenseAnalysisResponseDTO(
                domain.userId(),
                domain.startDate(),
                domain.endDate(),
                domain.totalExpenses(),
                domain.totalTransactions(),
                categories,
                days,
                months
        );
    }

    public record CategoryExpenseDTO(
            TransactionCategory category,
            BigDecimal totalAmount,
            BigDecimal percentage,
            long transactionCount
    ) {}

    public record DailyExpenseDTO(
            LocalDate date,
            BigDecimal totalAmount,
            long transactionCount
    ) {}

    public record MonthlyExpenseDTO(
            String yearMonth,
            BigDecimal totalAmount,
            long transactionCount
    ) {}
}

