package com.beca.ms_transaction.application.services;

import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
import com.beca.ms_transaction.application.ports.in.CreateTransactionUseCase;
import com.beca.ms_transaction.application.ports.in.GetAccountBalanceUseCase;
import com.beca.ms_transaction.application.ports.in.GetTransactionByIdUseCase;
import com.beca.ms_transaction.application.ports.in.SearchTransactionsUseCase;
import com.beca.ms_transaction.application.ports.out.AccountAuthorizationPort;
import com.beca.ms_transaction.application.ports.out.CurrencyExchangePort;
import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.application.ports.out.UserValidationPort;
import com.beca.ms_transaction.domain.exceptions.AccountNotFoundException;
import com.beca.ms_transaction.domain.exceptions.ExternalServiceUnavailableException;
import com.beca.ms_transaction.domain.exceptions.InvalidTransactionException;
import com.beca.ms_transaction.domain.exceptions.TransactionNotFoundException;
import com.beca.ms_transaction.domain.exceptions.UnauthorizedTransactionException;
import com.beca.ms_transaction.domain.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionService implements CreateTransactionUseCase, SearchTransactionsUseCase, GetTransactionByIdUseCase, GetAccountBalanceUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountAuthorizationPort accountAuthorizationPort;
    private final UserValidationPort userValidationPort;
    private final CurrencyExchangePort currencyExchangePort;

    public TransactionService(TransactionRepositoryPort transactionRepositoryPort,
                              AccountAuthorizationPort accountAuthorizationPort,
                              UserValidationPort userValidationPort,
                              CurrencyExchangePort currencyExchangePort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountAuthorizationPort = accountAuthorizationPort;
        this.userValidationPort = userValidationPort;
        this.currencyExchangePort = currencyExchangePort;
    }

    @Override
    public Transaction execute(CreateTransactionCommand command) {
        if (command == null || command.userId() == null || command.type() == null || command.amount() == null) {
            throw new InvalidTransactionException("Parâmetros obrigatórios da transação não fornecidos");
        }

        if (!userValidationPort.existsAndActive(command.userId())) {
            throw new InvalidTransactionException("Usuário originador não encontrado ou inativo no sistema. ID: " + command.userId());
        }

        if (command.type() == TransactionType.TRANSFER) {
            if (command.targetUserId() == null) {
                throw new InvalidTransactionException("O usuário de destino é obrigatório para transferências");
            }
            if (command.targetUserId().equals(command.userId())) {
                throw new InvalidTransactionException("Não é permitido realizar transferências para a própria conta");
            }
            if (!userValidationPort.existsAndActive(command.targetUserId())) {
                throw new InvalidTransactionException("Usuário destinatário não encontrado ou inativo no sistema. ID: " + command.targetUserId());
            }
        }

        if (command.type() == TransactionType.PURCHASE && command.category() == null) {
            throw new InvalidTransactionException("A categoria da despesa é obrigatória para compras");
        }

        // Depósitos são aprovados diretamente sem consulta a limites
        if (command.type() == TransactionType.DEPOSIT) {
            Transaction deposit = Transaction.createApproved(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId()
            );
            return transactionRepositoryPort.save(deposit);
        }

        // Para saques, transferências e compras: validação de saldo e limites via API Mock
        Optional<AccountBalance> accountOpt;
        try {
            accountOpt = accountAuthorizationPort.findAccountBalance(command.userId());
        } catch (ExternalServiceUnavailableException ex) {
            // Se a API Mock estiver indisponível, a transação não é aprovada às cegas; é rejeitada e registrada
            Transaction rejected = Transaction.createRejected(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId(),
                    "Serviço autorizador indisponível no momento"
            );
            return transactionRepositoryPort.save(rejected);
        }

        if (accountOpt.isEmpty()) {
            Transaction rejected = Transaction.createRejected(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId(),
                    "Conta do usuário não localizada no serviço de autorização"
            );
            return transactionRepositoryPort.save(rejected);
        }

        AccountBalance account = accountOpt.get();

        // Regra 1: Limite por transação
        if (account.transactionLimit() != null && command.amount().compareTo(account.transactionLimit()) > 0) {
            Transaction rejected = Transaction.createRejected(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId(),
                    String.format("Valor da transação excede o limite por transação permitido (Limite: R$ %.2f)", account.transactionLimit())
            );
            return transactionRepositoryPort.save(rejected);
        }

        // Regra 2: Saldo suficiente
        if (account.balance() != null && command.amount().compareTo(account.balance()) > 0) {
            Transaction rejected = Transaction.createRejected(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId(),
                    String.format("Saldo insuficiente para realizar a transação (Saldo disponível: R$ %.2f)", account.balance())
            );
            return transactionRepositoryPort.save(rejected);
        }

        // Regra 3: Limite diário acumulado
        if (account.dailyLimit() != null) {
            OffsetDateTime startOfDay = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
            OffsetDateTime endOfDay = startOfDay.plusDays(1);
            BigDecimal todayOutflows = transactionRepositoryPort.sumApprovedOutflowAmountForUserBetween(command.userId(), startOfDay, endOfDay);
            if (todayOutflows == null) {
                todayOutflows = BigDecimal.ZERO;
            }

            if (todayOutflows.add(command.amount()).compareTo(account.dailyLimit()) > 0) {
                Transaction rejected = Transaction.createRejected(
                        command.userId(),
                        command.type(),
                        command.amount(),
                        command.category(),
                        command.description(),
                        command.targetUserId(),
                        String.format("Transação excede o limite diário acumulado (Limite diário: R$ %.2f, Já utilizado hoje: R$ %.2f)", account.dailyLimit(), todayOutflows)
                );
                return transactionRepositoryPort.save(rejected);
            }
        }

        // Todas as validações aprovadas
        Transaction approved = Transaction.createApproved(
                command.userId(),
                command.type(),
                command.amount(),
                command.category(),
                command.description(),
                command.targetUserId()
        );
        return transactionRepositoryPort.save(approved);
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
    public TransactionDetails execute(UUID id, UUID authenticatedUserId) {
        if (id == null) {
            throw new InvalidTransactionException("O ID da transação é obrigatório");
        }

        Transaction transaction = transactionRepositoryPort.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada com ID: " + id));

        if (authenticatedUserId != null && !transaction.getUserId().equals(authenticatedUserId)) {
            throw new UnauthorizedTransactionException("Você não tem permissão para acessar esta transação");
        }

        LocalDate txDate = transaction.getCreatedAt() != null ? transaction.getCreatedAt().toLocalDate() : LocalDate.now();
        List<CurrencyConversion> conversions = getConversionsForAmount(transaction.getAmount(), txDate);

        return new TransactionDetails(transaction, conversions);
    }

    @Override
    public AccountBalance execute(UUID userId) {
        if (userId == null) {
            throw new InvalidTransactionException("O ID do usuário é obrigatório");
        }
        if (!userValidationPort.existsAndActive(userId)) {
            throw new InvalidTransactionException("Usuário não encontrado ou inativo no sistema. ID: " + userId);
        }

        return accountAuthorizationPort.findAccountBalance(userId)
                .orElseThrow(() -> new AccountNotFoundException("Conta do usuário não localizada no serviço de autorização"));
    }

    public List<CurrencyConversion> getConversionsForAmount(BigDecimal amountInBrl, LocalDate date) {
        if (amountInBrl == null || currencyExchangePort == null) {
            return List.of();
        }

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<CurrencyConversion> conversions = new ArrayList<>();

        for (String currency : List.of("USD", "EUR")) {
            Optional<CurrencyQuote> quoteOpt = currencyExchangePort.getQuote(currency, targetDate);
            if (quoteOpt.isPresent()) {
                CurrencyQuote quote = quoteOpt.get();
                if (quote.rate() != null && quote.rate().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal converted = amountInBrl.divide(quote.rate(), 2, RoundingMode.HALF_UP);
                    conversions.add(new CurrencyConversion(
                            currency,
                            amountInBrl,
                            converted,
                            quote.rate(),
                            quote.quoteDate()
                    ));
                }
            }
        }

        return conversions;
    }
}
