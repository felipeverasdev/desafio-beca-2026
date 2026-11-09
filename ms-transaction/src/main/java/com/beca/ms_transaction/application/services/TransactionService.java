package com.beca.ms_transaction.application.services;

import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
import com.beca.ms_transaction.application.ports.in.CreateTransactionUseCase;
import com.beca.ms_transaction.application.ports.in.GetTransactionByIdUseCase;
import com.beca.ms_transaction.application.ports.in.SearchTransactionsUseCase;
import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.domain.exceptions.InvalidTransactionException;
import com.beca.ms_transaction.domain.exceptions.TransactionNotFoundException;
import com.beca.ms_transaction.domain.exceptions.UnauthorizedTransactionException;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TransactionService implements CreateTransactionUseCase, SearchTransactionsUseCase, GetTransactionByIdUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public TransactionService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public Transaction execute(CreateTransactionCommand command) {
        // Bloco 5 Base: Criação aprovada direta
        // No Bloco 6, os validadores de MockAPI e BrasilAPI serão integrados nesta chamada
        Transaction transaction = Transaction.createApproved(
                command.userId(),
                command.type(),
                command.amount(),
                command.category(),
                command.description(),
                command.targetUserId()
        );

        return transactionRepositoryPort.save(transaction);
    }

    @Override
    public Page<Transaction> execute(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate,
                                    TransactionType type, Pageable pageable) {
        if (userId == null) {
            throw new InvalidTransactionException("O ID do usuário é obrigatório para a consulta");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionException("A data inicial não pode ser posterior à data final");
        }

        return transactionRepositoryPort.search(userId, startDate, endDate, type, pageable);
    }

    @Override
    public Transaction execute(UUID id, UUID authenticatedUserId) {
        if (id == null) {
            throw new InvalidTransactionException("O ID da transação é obrigatório");
        }

        Transaction transaction = transactionRepositoryPort.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transação com ID " + id + " não encontrada"));

        // Regra de segurança (Pedido 6): Isolamento de usuários
        if (authenticatedUserId != null &&
                !transaction.getUserId().equals(authenticatedUserId) &&
                (transaction.getTargetUserId() == null || !transaction.getTargetUserId().equals(authenticatedUserId))) {
            throw new UnauthorizedTransactionException("Acesso não autorizado para esta transação");
        }

        return transaction;
    }
}

