package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.exceptions.InvalidTransactionException;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateTransactionCommandTest {

    @Test
    @DisplayName("Deve criar comando válido com sucesso e categoria padrão OUTROS quando nula")
    void shouldCreateValidCommandWithDefaultCategory() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                null,
                "Depósito inicial",
                null
        );

        assertEquals(userId, command.userId());
        assertEquals(TransactionType.DEPOSIT, command.type());
        assertEquals(new BigDecimal("100.00"), command.amount());
        assertEquals(TransactionCategory.OUTROS, command.category());
    }

    @Test
    @DisplayName("Deve lançar exceção se userId for nulo")
    void shouldThrowExceptionWhenUserIdIsNull() {
        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(null, TransactionType.DEPOSIT, BigDecimal.TEN, null, null, null));
    }

    @Test
    @DisplayName("Deve lançar exceção se type for nulo")
    void shouldThrowExceptionWhenTypeIsNull() {
        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(UUID.randomUUID(), null, BigDecimal.TEN, null, null, null));
    }

    @Test
    @DisplayName("Deve lançar exceção se valor for nulo ou menor/igual a zero")
    void shouldThrowExceptionWhenAmountIsInvalid() {
        UUID userId = UUID.randomUUID();

        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(userId, TransactionType.DEPOSIT, null, null, null, null));

        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(userId, TransactionType.DEPOSIT, BigDecimal.ZERO, null, null, null));

        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(userId, TransactionType.DEPOSIT, new BigDecimal("-10.00"), null, null, null));
    }

    @Test
    @DisplayName("Deve lançar exceção se transferência não tiver usuário de destino")
    void shouldThrowExceptionWhenTransferHasNoTargetUser() {
        UUID userId = UUID.randomUUID();

        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(userId, TransactionType.TRANSFER, BigDecimal.TEN, null, null, null));
    }

    @Test
    @DisplayName("Deve lançar exceção se transferência tiver mesmo originador e destino")
    void shouldThrowExceptionWhenTransferHasSameSourceAndTarget() {
        UUID userId = UUID.randomUUID();

        assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(userId, TransactionType.TRANSFER, BigDecimal.TEN, null, null, userId));
    }
}

