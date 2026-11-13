package com.beca.ms_transaction.application.services;

import com.beca.ms_transaction.application.ports.in.AnalyzeExpensesUseCase;
import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
import com.beca.ms_transaction.application.ports.in.CreateTransactionUseCase;
import com.beca.ms_transaction.application.ports.in.ExportTransactionsCsvUseCase;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionService implements CreateTransactionUseCase, SearchTransactionsUseCase, GetTransactionByIdUseCase, GetAccountBalanceUseCase, AnalyzeExpensesUseCase, ExportTransactionsCsvUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

        // Depósitos são aprovados diretamente sem consulta a limites e creditados na MockAPI
        if (command.type() == TransactionType.DEPOSIT) {
            Transaction deposit = Transaction.createApproved(
                    command.userId(),
                    command.type(),
                    command.amount(),
                    command.category(),
                    command.description(),
                    command.targetUserId()
            );
            Transaction saved = transactionRepositoryPort.save(deposit);
            BigDecimal currentBalance = BigDecimal.ZERO;
            Optional<AccountBalance> accOpt = accountAuthorizationPort.findAccountBalance(command.userId());
            if (accOpt.isPresent() && accOpt.get().balance() != null) {
                currentBalance = accOpt.get().balance();
            }
            accountAuthorizationPort.updateBalance(command.userId(), currentBalance.add(command.amount()));
            return saved;
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

        // Regra 3: Limite diário acumulado (fuso horário America/Sao_Paulo)
        if (account.dailyLimit() != null) {
            LocalDate todaySp = LocalDate.now(ZONE_SP);
            OffsetDateTime startOfDay = todaySp.atStartOfDay(ZONE_SP).toOffsetDateTime();
            OffsetDateTime endOfDay = todaySp.plusDays(1).atStartOfDay(ZONE_SP).toOffsetDateTime();
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
        Transaction saved = transactionRepositoryPort.save(approved);

        // Atualiza (debita) saldo na MockAPI para a conta originadora
        BigDecimal currentBalance = account.balance() != null ? account.balance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.subtract(command.amount());
        accountAuthorizationPort.updateBalance(command.userId(), newBalance);

        // Se for transferência, credita o destinatário se possuir conta na MockAPI
        if (command.type() == TransactionType.TRANSFER && command.targetUserId() != null) {
            accountAuthorizationPort.findAccountBalance(command.targetUserId()).ifPresent(targetAcc -> {
                BigDecimal targetCurrent = targetAcc.balance() != null ? targetAcc.balance() : BigDecimal.ZERO;
                accountAuthorizationPort.updateBalance(command.targetUserId(), targetCurrent.add(command.amount()));
            });
        }

        return saved;
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

        LocalDate txDate = transaction.getCreatedAt() != null
                ? transaction.getCreatedAt().atZoneSameInstant(ZONE_SP).toLocalDate()
                : LocalDate.now(ZONE_SP);
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

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZONE_SP);
        List<CurrencyConversion> conversions = new ArrayList<>();

        for (String currency : List.of("USD", "EUR")) {
            try {
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
            } catch (Exception ex) {
                log.warn("Falha ao obter cotação de câmbio para {} (data {}): {}", currency, targetDate, ex.getMessage());
            }
        }

        return conversions;
    }

    @Override
    public ExpenseAnalysis execute(UUID userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null) {
            throw new InvalidTransactionException("O ID do usuário é obrigatório para a análise de despesas");
        }
        if (!userValidationPort.existsAndActive(userId)) {
            throw new InvalidTransactionException("Usuário não encontrado ou inativo no sistema. ID: " + userId);
        }

        LocalDate effectiveStart = startDate;
        LocalDate effectiveEnd = endDate;

        // Regra do PO: Período padrão, quando não informado: o mês atual.
        LocalDate today = LocalDate.now(ZONE_SP);
        if (effectiveStart == null && effectiveEnd == null) {
            effectiveStart = today.withDayOfMonth(1);
            effectiveEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        } else if (effectiveStart == null) {
            effectiveStart = effectiveEnd.withDayOfMonth(1);
        } else if (effectiveEnd == null) {
            effectiveEnd = effectiveStart.with(TemporalAdjusters.lastDayOfMonth());
        }

        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new InvalidTransactionException("A data inicial não pode ser posterior à data final");
        }

        OffsetDateTime startDateTime = effectiveStart.atStartOfDay(ZONE_SP).toOffsetDateTime();
        OffsetDateTime endDateTime = effectiveEnd.atTime(23, 59, 59, 999999999).atZone(ZONE_SP).toOffsetDateTime();

        List<Transaction> transactions = transactionRepositoryPort.findForAnalysis(userId, startDateTime, endDateTime);

        BigDecimal totalExpenses = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrupamento por Categoria (ordenado por maior gasto)
        Map<TransactionCategory, List<Transaction>> byCategoryMap = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory() : TransactionCategory.OUTROS));

        List<ExpenseAnalysis.CategoryExpenseSummary> categorySummaries = byCategoryMap.entrySet().stream()
                .map(entry -> {
                    TransactionCategory category = entry.getKey();
                    BigDecimal categoryTotal = entry.getValue().stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? categoryTotal.multiply(new BigDecimal("100")).divide(totalExpenses, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new ExpenseAnalysis.CategoryExpenseSummary(category, categoryTotal, percentage, entry.getValue().size());
                })
                .sorted((a, b) -> b.totalAmount().compareTo(a.totalAmount()))
                .toList();

        // Agrupamento por Dia (ordem cronológica)
        Map<LocalDate, List<Transaction>> byDayMap = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().atZoneSameInstant(ZONE_SP).toLocalDate()));

        List<ExpenseAnalysis.DailyExpenseSummary> dailySummaries = byDayMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal dayTotal = entry.getValue().stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExpenseAnalysis.DailyExpenseSummary(entry.getKey(), dayTotal, entry.getValue().size());
                })
                .sorted(Comparator.comparing(ExpenseAnalysis.DailyExpenseSummary::date))
                .toList();

        // Agrupamento por Mês (ordem cronológica "YYYY-MM")
        Map<String, List<Transaction>> byMonthMap = transactions.stream()
                .collect(Collectors.groupingBy(t -> {
                    LocalDate d = t.getCreatedAt().atZoneSameInstant(ZONE_SP).toLocalDate();
                    return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
                }));

        List<ExpenseAnalysis.MonthlyExpenseSummary> monthlySummaries = byMonthMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal monthTotal = entry.getValue().stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExpenseAnalysis.MonthlyExpenseSummary(entry.getKey(), monthTotal, entry.getValue().size());
                })
                .sorted(Comparator.comparing(ExpenseAnalysis.MonthlyExpenseSummary::yearMonth))
                .toList();

        return new ExpenseAnalysis(
                userId,
                effectiveStart,
                effectiveEnd,
                totalExpenses,
                transactions.size(),
                categorySummaries,
                dailySummaries,
                monthlySummaries
        );
    }

    @Override
    public byte[] execute(UUID userId, LocalDate startDate, LocalDate endDate, TransactionType type) {
        if (userId == null) {
            throw new InvalidTransactionException("O ID do usuário é obrigatório para exportar o relatório");
        }
        if (!userValidationPort.existsAndActive(userId)) {
            throw new InvalidTransactionException("Usuário não encontrado ou inativo no sistema. ID: " + userId);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionException("A data inicial não pode ser posterior à data final");
        }

        OffsetDateTime startDateTime = startDate != null ? startDate.atStartOfDay(ZONE_SP).toOffsetDateTime() : null;
        OffsetDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59, 999999999).atZone(ZONE_SP).toOffsetDateTime() : null;

        List<Transaction> transactions = transactionRepositoryPort.findAllByFilters(userId, startDateTime, endDateTime, type);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat currencyFormat = new DecimalFormat("R$ #,##0.00", symbols);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // UTF-8 BOM (Byte Order Mark) para garantir compatibilidade com Microsoft Excel no Windows
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        writer.println("ID;Data/Hora;Tipo;Categoria;Descrição;Valor;Status;Destinatário;Motivo da Rejeição");

        for (Transaction tx : transactions) {
            String formattedDate = tx.getCreatedAt() != null ? tx.getCreatedAt().atZoneSameInstant(ZONE_SP).format(CSV_DATE_FORMAT) : "";
            String typeStr = tx.getType() != null ? tx.getType().name() : "";
            String catStr = tx.getCategory() != null ? tx.getCategory().name() : "";
            String descStr = escapeCsv(tx.getDescription());
            String amountStr = tx.getAmount() != null ? currencyFormat.format(tx.getAmount()) : "R$ 0,00";
            String statusStr = tx.getStatus() != null ? tx.getStatus().name() : "";
            String targetStr = tx.getTargetUserId() != null ? tx.getTargetUserId().toString() : "";
            String reasonStr = escapeCsv(tx.getRejectionReason());

            writer.printf("%s;%s;%s;%s;%s;%s;%s;%s;%s%n",
                    tx.getId(),
                    formattedDate,
                    typeStr,
                    catStr,
                    descStr,
                    amountStr,
                    statusStr,
                    targetStr,
                    reasonStr
            );
        }

        writer.flush();
        return baos.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = value.replace("\"", "\"\"");
        if (clean.contains(";") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
