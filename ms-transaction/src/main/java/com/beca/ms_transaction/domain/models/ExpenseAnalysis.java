package com.beca.ms_transaction.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseAnalysis(
        UUID userId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalExpenses,
        long totalTransactions,
        List<CategoryExpenseSummary> byCategory,
        List<DailyExpenseSummary> byDay,
        List<MonthlyExpenseSummary> byMonth
) {
    public ExpenseAnalysis {
        byCategory = (byCategory != null) ? List.copyOf(byCategory) : List.of();
        byDay = (byDay != null) ? List.copyOf(byDay) : List.of();
        byMonth = (byMonth != null) ? List.copyOf(byMonth) : List.of();
    }

    public record CategoryExpenseSummary(
            TransactionCategory category,
            BigDecimal totalAmount,
            BigDecimal percentage,
            long transactionCount
    ) {}

    public record DailyExpenseSummary(
            LocalDate date,
            BigDecimal totalAmount,
            long transactionCount
    ) {}

    public record MonthlyExpenseSummary(
            String yearMonth,
            BigDecimal totalAmount,
            long transactionCount
    ) {}
}

