package com.beca.ms_transaction.infrastructure.adapters.in.rest;

import com.beca.ms_transaction.application.ports.in.CreateTransactionUseCase;
import com.beca.ms_transaction.application.ports.in.GetAccountBalanceUseCase;
import com.beca.ms_transaction.application.ports.in.GetTransactionByIdUseCase;
import com.beca.ms_transaction.application.ports.in.SearchTransactionsUseCase;
import com.beca.ms_transaction.domain.models.AccountBalance;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionDetails;
import com.beca.ms_transaction.domain.models.TransactionType;
import com.beca.ms_transaction.infrastructure.adapters.in.rest.dto.AccountBalanceResponseDTO;
import com.beca.ms_transaction.infrastructure.adapters.in.rest.dto.TransactionRequestDTO;
import com.beca.ms_transaction.infrastructure.adapters.in.rest.dto.TransactionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase,
                                 SearchTransactionsUseCase searchTransactionsUseCase,
                                 GetTransactionByIdUseCase getTransactionByIdUseCase,
                                 GetAccountBalanceUseCase getAccountBalanceUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
        this.searchTransactionsUseCase = searchTransactionsUseCase;
        this.getTransactionByIdUseCase = getTransactionByIdUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova transação financeira",
            description = "Valida o usuário no ms-user, consulta saldo e limites no MockAPI e grava a transação como aprovada ou rejeitada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transação processada com sucesso (status APPROVED ou REJECTED gravado)")
    })
    public ResponseEntity<TransactionResponseDTO> create(@Valid @RequestBody TransactionRequestDTO dto) {
        Transaction created = createTransactionUseCase.execute(dto.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponseDTO.fromDomain(created));
    }

    @GetMapping("/balance/{userId}")
    @Operation(summary = "Consultar saldo e limites do usuário na API Mock",
            description = "Consulta saldo atual, limite diário e limite por transação do usuário diretamente do serviço externo MockAPI.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo e limites retornados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada no serviço de autorização")
    })
    public ResponseEntity<AccountBalanceResponseDTO> getBalance(@PathVariable UUID userId) {
        AccountBalance balance = getAccountBalanceUseCase.execute(userId);
        return ResponseEntity.ok(AccountBalanceResponseDTO.fromDomain(balance));
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
    @Operation(summary = "Buscar transação por ID",
            description = "Retorna os detalhes da transação e os valores convertidos em USD e EUR pela BrasilAPI.")
    public ResponseEntity<TransactionResponseDTO> getById(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID authenticatedUserId
    ) {
        TransactionDetails details = getTransactionByIdUseCase.execute(id, authenticatedUserId);
        return ResponseEntity.ok(TransactionResponseDTO.fromDomain(details.transaction(), details.conversions()));
    }
}

