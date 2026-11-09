package com.beca.ms_transaction.application.services;

import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.domain.exceptions.InvalidTransactionException;
import com.beca.ms_transaction.domain.exceptions.TransactionNotFoundException;
import com.beca.ms_transaction.domain.exceptions.UnauthorizedTransactionException;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Deve criar e salvar transação com sucesso")
    void shouldCreateAndSaveTransaction() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.DEPOSIT,
                new BigDecimal("250.00"),
                TransactionCategory.OUTROS,
                "Depósito bancário",
                null
        );

        Transaction savedTransaction = new Transaction(
                UUID.randomUUID(),
                userId,
                TransactionType.DEPOSIT,
                TransactionStatus.APPROVED,
                new BigDecimal("250.00"),
                TransactionCategory.OUTROS,
                "Depósito bancário",
                null,
                null,
                OffsetDateTime.now()
        );

        when(transactionRepositoryPort.save(any(Transaction.class))).thenReturn(savedTransaction);

        Transaction result = transactionService.execute(command);

        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        verify(transactionRepositoryPort, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve buscar transações paginadas com sucesso")
    void shouldSearchTransactions() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx = new Transaction(UUID.randomUUID(), userId, TransactionType.DEPOSIT, TransactionStatus.APPROVED,
                BigDecimal.TEN, TransactionCategory.OUTROS, "Teste", null, null, OffsetDateTime.now());
        Page<Transaction> page = new PageImpl<>(List.of(tx));

        when(transactionRepositoryPort.search(eq(userId), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<Transaction> result = transactionService.execute(userId, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(transactionRepositoryPort, times(1)).search(eq(userId), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("Deve lançar exceção se data inicial for posterior à data final na busca")
    void shouldThrowExceptionWhenStartDateAfterEndDate() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.minusDays(1);
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.execute(userId, start, end, null, pageable));
    }

    @Test
    @DisplayName("Deve buscar transação por ID com sucesso para usuário autorizado")
    void shouldGetTransactionByIdWhenAuthorized() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, userId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                BigDecimal.TEN, TransactionCategory.ALIMENTACAO, "Lanche", null, null, OffsetDateTime.now());

        when(transactionRepositoryPort.findById(txId)).thenReturn(Optional.of(tx));

        Transaction result = transactionService.execute(txId, userId);

        assertEquals(txId, result.getId());
        assertEquals(userId, result.getUserId());
    }

    @Test
    @DisplayName("Deve lançar TransactionNotFoundException se transação não existir")
    void shouldThrowExceptionWhenTransactionNotFound() {
        UUID txId = UUID.randomUUID();
        when(transactionRepositoryPort.findById(txId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
                transactionService.execute(txId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedTransactionException se usuário não for o dono da transação")
    void shouldThrowUnauthorizedExceptionWhenUserIsNotOwner() {
        UUID txId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        Transaction tx = new Transaction(txId, ownerId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                BigDecimal.TEN, TransactionCategory.ALIMENTACAO, "Lanche", null, null, OffsetDateTime.now());

        when(transactionRepositoryPort.findById(txId)).thenReturn(Optional.of(tx));

        assertThrows(UnauthorizedTransactionException.class, () ->
                transactionService.execute(txId, strangerId));
    }
}

