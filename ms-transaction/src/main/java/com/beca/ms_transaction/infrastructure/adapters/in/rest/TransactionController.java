package com.beca.ms_transaction.infrastructure.adapters.in.rest;

import com.beca.ms_transaction.application.ports.in.CreateTransactionUseCase;
import com.beca.ms_transaction.application.ports.in.GetTransactionByIdUseCase;
import com.beca.ms_transaction.application.ports.in.SearchTransactionsUseCase;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionType;
import com.beca.ms_transaction.infrastructure.adapters.in.rest.dto.TransactionRequestDTO;
import com.beca.ms_transaction.infrastructure.adapters.in.rest.dto.TransactionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints de gerenciamento e consulta de transações financeiras")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;
    private final SearchTransactionsUseCase searchTransactionsUseCase;
    private final GetTransactionByIdUseCase getTransactionByIdUseCase;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                 SearchTransactionsUseCase searchTransactionsUseCase,
                                 GetTransactionByIdUseCase getTransactionByIdUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.searchTransactionsUseCase = searchTransactionsUseCase;
        this.getTransactionByIdUseCase = getTransactionByIdUseCase;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova transação financeira")
    public ResponseEntity<TransactionResponseDTO> create(@Valid @RequestBody TransactionRequestDTO dto) {
        Transaction created = createTransactionUseCase.execute(dto.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponseDTO.fromDomain(created));
    }

    @GetMapping
    @Operation(summary = "Consultar transações com filtros por data e tipo")
    public ResponseEntity<Page<TransactionResponseDTO>> search(
            @RequestParam("userId") UUID userId,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(value = "type", required = false) TransactionType type,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Transaction> result = searchTransactionsUseCase.execute(userId, startDate, endDate, type, pageable);
        return ResponseEntity.ok(result.map(TransactionResponseDTO::fromDomain));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transação por ID")
    public ResponseEntity<TransactionResponseDTO> getById(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID authenticatedUserId
    ) {
        Transaction transaction = getTransactionByIdUseCase.execute(id, authenticatedUserId);
        return ResponseEntity.ok(TransactionResponseDTO.fromDomain(transaction));
    }
}

