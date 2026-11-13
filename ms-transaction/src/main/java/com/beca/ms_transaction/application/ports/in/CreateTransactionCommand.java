package com.beca.ms_transaction.application.ports.in;

import com.beca.ms_transaction.domain.exceptions.InvalidTransactionException;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionCommand(
        UUID userId,
        TransactionType type,
        BigDecimal amount,
        TransactionCategory category,
        String description,
        UUID targetUserId
) {
    public CreateTransactionCommand {
        if (userId == null) {
            throw new InvalidTransactionException("O ID do usuário originador é obrigatório");
        }
        if (type == null) {
            throw new InvalidTransactionException("O tipo da transação é obrigatório");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("O valor da transação deve ser maior que zero");
        }
        if (type == TransactionType.TRANSFER) {
            if (targetUserId == null) {
                throw new InvalidTransactionException("O ID do usuário de destino é obrigatório para transferências");
            }
            if (targetUserId.equals(userId)) {
                throw new InvalidTransactionException("Não é permitido transferir para a mesma conta");
            }
        }
        if (type == TransactionType.PURCHASE && category == null) {
            throw new InvalidTransactionException("A categoria da despesa é obrigatória para compras");
        }
        if (category == null) {
            category = TransactionCategory.OUTROS;
        }
    }
}

