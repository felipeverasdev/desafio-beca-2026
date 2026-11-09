package com.beca.ms_transaction.domain.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    @DisplayName("Deve criar uma transação aprovada com status APPROVED e ID nulo para geração pelo banco")
    void shouldCreateApprovedTransaction() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        Transaction transaction = Transaction.createApproved(
                userId,
                TransactionType.PURCHASE,
                amount,
                TransactionCategory.ALIMENTACAO,
                "Almoço",
                null
        );

        assertNull(transaction.getId());
        assertEquals(userId, transaction.getUserId());
        assertEquals(TransactionType.PURCHASE, transaction.getType());
        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(amount, transaction.getAmount());
        assertEquals(TransactionCategory.ALIMENTACAO, transaction.getCategory());
        assertEquals("Almoço", transaction.getDescription());
        assertNull(transaction.getTargetUserId());
        assertNull(transaction.getRejectionReason());
    }

    @Test
    @DisplayName("Deve criar uma transação rejeitada com motivo")
    void shouldCreateRejectedTransaction() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");

        Transaction transaction = Transaction.createRejected(
                userId,
                TransactionType.WITHDRAWAL,
                amount,
                TransactionCategory.OUTROS,
                "Saque alto",
                null,
                "Limite diário excedido"
        );

        assertNull(transaction.getId());
        assertEquals(userId, transaction.getUserId());
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals(TransactionStatus.REJECTED, transaction.getStatus());
        assertEquals("Limite diário excedido", transaction.getRejectionReason());
    }
}

