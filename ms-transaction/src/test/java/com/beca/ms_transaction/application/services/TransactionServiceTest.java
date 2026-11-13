package com.beca.ms_transaction.application.services;

import com.beca.ms_transaction.application.ports.in.CreateTransactionCommand;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private AccountAuthorizationPort accountAuthorizationPort;

    @Mock
    private UserValidationPort userValidationPort;

    @Mock
    private CurrencyExchangePort currencyExchangePort;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Deve criar e salvar depósito com aprovação direta sem consulta de limites")
    void shouldCreateAndSaveDepositDirectly() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.DEPOSIT,
                new BigDecimal("250.00"),
                TransactionCategory.OUTROS,
                "Depósito bancário",
                null
        );

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(
                new AccountBalance(userId, new BigDecimal("100.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"))
        ));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            return new Transaction(UUID.randomUUID(), t.getUserId(), t.getType(), t.getStatus(), t.getAmount(), t.getCategory(), t.getDescription(), t.getTargetUserId(), t.getRejectionReason(), OffsetDateTime.now());
        });

        Transaction result = transactionService.execute(command);

        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        assertEquals(new BigDecimal("250.00"), result.getAmount());
        verify(transactionRepositoryPort, times(1)).save(any(Transaction.class));
        verify(accountAuthorizationPort, times(1)).updateBalance(userId, new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("Deve lançar InvalidTransactionException quando usuário originador não existe no ms-user")
    void shouldThrowWhenOriginUserNotFound() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.DEPOSIT,
                new BigDecimal("100.00"),
                TransactionCategory.OUTROS,
                "Depósito",
                null
        );

        when(userValidationPort.existsAndActive(userId)).thenReturn(false);

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.execute(command));
        assertTrue(ex.getMessage().contains("não encontrado ou inativo"));
    }

    @Test
    @DisplayName("Deve lançar InvalidTransactionException quando destino da transferência não existe")
    void shouldThrowWhenTransferTargetUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.TRANSFER,
                new BigDecimal("50.00"),
                TransactionCategory.OUTROS,
                "Pix",
                targetId
        );

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(userValidationPort.existsAndActive(targetId)).thenReturn(false);

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                transactionService.execute(command));
        assertTrue(ex.getMessage().contains("destinatário não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar InvalidTransactionException ao transferir para a própria conta")
    void shouldThrowWhenTransferToSameAccount() {
        UUID userId = UUID.randomUUID();

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class, () ->
                new CreateTransactionCommand(
                        userId,
                        TransactionType.TRANSFER,
                        new BigDecimal("50.00"),
                        TransactionCategory.OUTROS,
                        "Pix",
                        userId
                ));
        assertTrue(ex.getMessage().contains("mesma conta"));
    }

    @Test
    @DisplayName("Deve aprovar compra quando saldo e limites forem suficientes (KAN-9)")
    void shouldApprovePurchaseWhenLimitsAndBalanceAreSufficient() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.PURCHASE,
                new BigDecimal("150.00"),
                TransactionCategory.ALIMENTACAO,
                "Mercado",
                null
        );

        AccountBalance balance = new AccountBalance(userId, new BigDecimal("1000.00"), new BigDecimal("500.00"), new BigDecimal("300.00"));
        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.sumApprovedOutflowAmountForUserBetween(eq(userId), any(), any())).thenReturn(new BigDecimal("100.00"));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.execute(command);

        assertEquals(TransactionStatus.APPROVED, result.getStatus());
        assertNull(result.getRejectionReason());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        verify(accountAuthorizationPort, times(1)).updateBalance(userId, new BigDecimal("850.00"));
    }

    @Test
    @DisplayName("Deve rejeitar transação quando valor exceder limite por transação (KAN-9)")
    void shouldRejectWhenExceedsTransactionLimit() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.PURCHASE,
                new BigDecimal("350.00"),
                TransactionCategory.ALIMENTACAO,
                "Mercado",
                null
        );

        AccountBalance balance = new AccountBalance(userId, new BigDecimal("1000.00"), new BigDecimal("500.00"), new BigDecimal("300.00"));
        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.execute(command);

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getRejectionReason().contains("excede o limite por transação permitido"));
    }

    @Test
    @DisplayName("Deve rejeitar transação quando valor acumulado no dia exceder limite diário (KAN-9)")
    void shouldRejectWhenExceedsDailyLimit() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.PURCHASE,
                new BigDecimal("150.00"),
                TransactionCategory.ALIMENTACAO,
                "Mercado",
                null
        );

        AccountBalance balance = new AccountBalance(userId, new BigDecimal("1000.00"), new BigDecimal("500.00"), new BigDecimal("300.00"));
        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.sumApprovedOutflowAmountForUserBetween(eq(userId), any(), any())).thenReturn(new BigDecimal("400.00"));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.execute(command);

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getRejectionReason().contains("excede o limite diário"));
    }

    @Test
    @DisplayName("Deve rejeitar transação quando saldo em conta for insuficiente (KAN-9)")
    void shouldRejectWhenBalanceInsufficient() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.WITHDRAWAL,
                new BigDecimal("250.00"),
                TransactionCategory.OUTROS,
                "Saque 24h",
                null
        );

        AccountBalance balance = new AccountBalance(userId, new BigDecimal("100.00"), new BigDecimal("500.00"), new BigDecimal("300.00"));
        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.execute(command);

        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getRejectionReason().contains("Saldo insuficiente para realizar a transação"));
    }

    @Test
    @DisplayName("Deve lançar ExternalServiceUnavailableException quando MockAPI estiver fora do ar")
    void shouldThrowExternalServiceUnavailableExceptionWhenMockApiUnavailable() {
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.PURCHASE,
                new BigDecimal("50.00"),
                TransactionCategory.ALIMENTACAO,
                "Padaria",
                null
        );

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenThrow(new ExternalServiceUnavailableException("Timeout MockAPI"));

        assertThrows(ExternalServiceUnavailableException.class, () -> transactionService.execute(command));
        verify(transactionRepositoryPort, never()).save(any());
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
    @DisplayName("Deve buscar transação por ID com conversões de moeda (KAN-10 & KAN-11)")
    void shouldGetTransactionByIdWithConversionsWhenAuthorized() {
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, userId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                new BigDecimal("100.00"), TransactionCategory.ALIMENTACAO, "Lanche", null, null, OffsetDateTime.now());

        when(transactionRepositoryPort.findById(txId)).thenReturn(Optional.of(tx));
        when(currencyExchangePort.getQuote(eq("USD"), any())).thenReturn(Optional.of(new CurrencyQuote("USD", new BigDecimal("5.00"), LocalDate.now())));
        when(currencyExchangePort.getQuote(eq("EUR"), any())).thenReturn(Optional.of(new CurrencyQuote("EUR", new BigDecimal("6.00"), LocalDate.now())));

        TransactionDetails result = transactionService.execute(txId, userId);

        assertNotNull(result);
        assertEquals(txId, result.transaction().getId());
        assertEquals(2, result.conversions().size());

        CurrencyConversion usd = result.conversions().stream().filter(c -> c.currency().equals("USD")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("20.00"), usd.convertedAmount());

        CurrencyConversion eur = result.conversions().stream().filter(c -> c.currency().equals("EUR")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("16.67"), eur.convertedAmount());
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

    @Test
    @DisplayName("Deve consultar saldo do usuário na MockAPI com sucesso (GetAccountBalanceUseCase)")
    void shouldGetAccountBalanceSuccessfully() {
        UUID userId = UUID.randomUUID();
        AccountBalance balance = new AccountBalance(userId, new BigDecimal("2500.00"), new BigDecimal("1000.00"), new BigDecimal("500.00"));

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.of(balance));

        AccountBalance result = transactionService.execute(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(new BigDecimal("2500.00"), result.balance());
    }

    @Test
    @DisplayName("Deve lançar AccountNotFoundException ao consultar saldo de conta inexistente")
    void shouldThrowAccountNotFoundExceptionWhenBalanceNotFound() {
        UUID userId = UUID.randomUUID();

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(accountAuthorizationPort.findAccountBalance(userId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
                transactionService.execute(userId));
    }

    @Test
    @DisplayName("Deve analisar despesas no período padrão (mês atual) com agrupamento e percentuais (KAN-12)")
    void shouldAnalyzeExpensesWithDefaultPeriodAndPercentages() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Transaction tx1 = new Transaction(UUID.randomUUID(), userId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                new BigDecimal("150.00"), TransactionCategory.ALIMENTACAO, "Mercado", null, null, now);
        Transaction tx2 = new Transaction(UUID.randomUUID(), userId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                new BigDecimal("50.00"), TransactionCategory.TRANSPORTE, "Uber", null, null, now);

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(transactionRepositoryPort.findForAnalysis(eq(userId), any(), any()))
                .thenReturn(List.of(tx1, tx2));

        ExpenseAnalysis analysis = transactionService.execute(userId, null, null);

        assertNotNull(analysis);
        assertEquals(userId, analysis.userId());
        assertEquals(new BigDecimal("200.00"), analysis.totalExpenses());
        assertEquals(2, analysis.totalTransactions());
        assertEquals(2, analysis.byCategory().size());

        ExpenseAnalysis.CategoryExpenseSummary alimentacao = analysis.byCategory().stream()
                .filter(c -> c.category() == TransactionCategory.ALIMENTACAO).findFirst().orElseThrow();
        assertEquals(new BigDecimal("150.00"), alimentacao.totalAmount());
        assertEquals(new BigDecimal("75.00"), alimentacao.percentage());
        assertEquals(1, alimentacao.transactionCount());

        ExpenseAnalysis.CategoryExpenseSummary transporte = analysis.byCategory().stream()
                .filter(c -> c.category() == TransactionCategory.TRANSPORTE).findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.00"), transporte.totalAmount());
        assertEquals(new BigDecimal("25.00"), transporte.percentage());
        assertEquals(1, transporte.transactionCount());

        assertEquals(1, analysis.byDay().size());
        assertEquals(1, analysis.byMonth().size());
    }

    @Test
    @DisplayName("Deve lançar InvalidTransactionException quando data inicial for posterior à data final na análise")
    void shouldThrowWhenStartDateAfterEndDateOnAnalysis() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now();

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.execute(userId, start, end));
    }

    @Test
    @DisplayName("Deve exportar relatório de transações em CSV compatível com Excel pt-BR (KAN-20)")
    void shouldExportTransactionsCsvSuccessfully() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Transaction tx1 = new Transaction(UUID.randomUUID(), userId, TransactionType.PURCHASE, TransactionStatus.APPROVED,
                new BigDecimal("1234.56"), TransactionCategory.ALIMENTACAO, "Compras do mês", null, null, now);
        Transaction tx2 = new Transaction(UUID.randomUUID(), userId, TransactionType.WITHDRAWAL, TransactionStatus.REJECTED,
                new BigDecimal("5000.00"), TransactionCategory.OUTROS, "Saque alto", null, "Limite diário excedido", now);

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);
        when(transactionRepositoryPort.findAllByFilters(eq(userId), any(), any(), any()))
                .thenReturn(List.of(tx1, tx2));

        byte[] csvBytes = transactionService.execute(userId, LocalDate.now().minusDays(10), LocalDate.now(), null);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 3);
        // Verifica UTF-8 BOM
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvContent = new String(csvBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("ID;Data/Hora;Tipo;Categoria;Descrição;Valor;Status;Destinatário;Motivo da Rejeição"));
        assertTrue(csvContent.contains("PURCHASE"));
        assertTrue(csvContent.contains("ALIMENTACAO"));
        assertTrue(csvContent.contains("Compras do mês"));
        assertTrue(csvContent.contains("R$ 1.234,56") || csvContent.contains("1.234,56"));
        assertTrue(csvContent.contains("WITHDRAWAL"));
        assertTrue(csvContent.contains("REJECTED"));
        assertTrue(csvContent.contains("Limite diário excedido"));
    }

    @Test
    @DisplayName("Deve lançar InvalidTransactionException quando data inicial for posterior à data final na exportação CSV")
    void shouldThrowWhenStartDateAfterEndDateOnCsvExport() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now();

        when(userValidationPort.existsAndActive(userId)).thenReturn(true);

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.execute(userId, start, end, null));
    }
}
