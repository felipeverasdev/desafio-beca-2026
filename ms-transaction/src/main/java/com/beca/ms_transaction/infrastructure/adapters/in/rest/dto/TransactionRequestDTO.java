package com.beca.ms_transaction.infrastructure.adapters.in.rest.dto;

import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
import com.beca.ms_transaction.domain.models.TransactionCategory;
import com.beca.ms_transaction.domain.models.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequestDTO(
        @NotNull(message = "O ID do usuário originador é obrigatório")
        UUID userId,

        @NotNull(message = "O tipo da transação é obrigatório")
        TransactionType type,

        @NotNull(message = "O valor da transação é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal amount,

        TransactionCategory category,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String description,

        UUID targetUserId
) {
    public CreateTransactionCommand toCommand() {
        return new CreateTransactionCommand(userId, type, amount, category, description, targetUserId);
    }
}

