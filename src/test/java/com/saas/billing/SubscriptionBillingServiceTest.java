package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.FakeNotificationService.Notification;
import com.saas.billing.FakePaymentGateway.Charge;
import com.saas.billing.InMemoryInvoiceRepository.SavedInvoice;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SubscriptionBillingServiceTest {

    private static final LocalDate JUNHO = PricingScenarios.JUNHO;
    private static final LocalDate NOVEMBRO = PricingScenarios.NOVEMBRO;
    private static final LocalDate CINCO_MESES_ANTES_DE_JUNHO = LocalDate.of(2026, 1, 10);

    private static final Customer ATIVO = new Customer("c1", "ana@cliente.com", "ACTIVE");

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository().with(ATIVO);
    private final InMemoryInvoiceRepository invoices = new InMemoryInvoiceRepository();
    private final FakeNotificationService notifications = new FakeNotificationService();

    private static Clock fixedAt(LocalDate date) {
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    private SubscriptionBillingService service(PaymentGateway gateway, LocalDate today) {
        return new SubscriptionBillingService(customers, invoices, gateway, notifications, fixedAt(today));
    }

    private static BillingCommand cobranca(String customerId, String promoCode) {
        return new BillingCommand(customerId, "PRO", Money.of("100"), CINCO_MESES_ANTES_DE_JUNHO,
                0, promoCode, false, "tok_123");
    }

    @Test
    void pagamentoAprovadoGravaFaturaPagaEAvisaOCliente() {
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = service(gateway, JUNHO).processMonthlyBilling(cobranca("c1", null));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100.00",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(List.of(new Charge("tok_123", Money.of("100"))), gateway.charges());
        assertEquals(List.of(new SavedInvoice("c1", Money.of("100"), InvoiceRepository.Status.PAID)), invoices.saved());
        assertEquals(List.of(new Notification("ana@cliente.com", Money.of("100"))), notifications.sent());
    }

    @Test
    void pagamentoRecusadoGravaFaturaComFalhaEAvisaMesmoAssim() {
        List<String> logs = service(FakePaymentGateway.declining(), JUNHO).processMonthlyBilling(cobranca("c1", null));

        assertEquals(List.of(
                "Falha no pagamento do cartão para: c1",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(List.of(new SavedInvoice("c1", Money.of("100"), InvoiceRepository.Status.FAILED)), invoices.saved());
        assertEquals(1, notifications.sent().size());
    }

    @Test
    void clienteInativoNaoECobrado() {
        customers.with(new Customer("c2", "bia@cliente.com", "SUSPENDED"));
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = service(gateway, JUNHO).processMonthlyBilling(cobranca("c2", null));

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: c2"), logs);
        assertTrue(gateway.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void clienteInexistenteNaoGeraNada() {
        FakePaymentGateway gateway = FakePaymentGateway.approving();

        List<String> logs = service(gateway, JUNHO).processMonthlyBilling(cobranca("nao-existe", null));

        assertTrue(logs.isEmpty());
        assertTrue(gateway.charges().isEmpty());
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void falhaDeInfraestruturaViraLogDeErro() {
        CustomerRepository foraDoAr = id -> {
            throw new InfrastructureException(new SQLException("conexão recusada"));
        };
        SubscriptionBillingService service = new SubscriptionBillingService(
                foraDoAr, invoices, FakePaymentGateway.approving(), notifications, fixedAt(JUNHO));

        List<String> logs = service.processMonthlyBilling(cobranca("c1", null));

        assertEquals(List.of("Erro fatal no processamento: conexão recusada"), logs);
    }

    @Test
    void falhaAoGravarFaturaDepoisDoPagamentoFicaRegistradaNoLog() {
        InvoiceRepository bancoForaDoAr = (customerId, amount, status) -> {
            throw new InfrastructureException(new SQLException("timeout"));
        };
        SubscriptionBillingService service = new SubscriptionBillingService(
                customers, bancoForaDoAr, FakePaymentGateway.approving(), notifications, fixedAt(JUNHO));

        List<String> logs = service.processMonthlyBilling(cobranca("c1", null));

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 100.00",
                "Erro fatal no processamento: timeout"), logs);
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    void erroQueNaoEDeInfraestruturaNaoESilenciado() {
        PaymentGateway comBug = (token, amount) -> {
            throw new IllegalStateException("bug no gateway");
        };
        SubscriptionBillingService service = new SubscriptionBillingService(
                customers, invoices, comBug, notifications, fixedAt(JUNHO));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.processMonthlyBilling(cobranca("c1", null)));
        assertEquals("bug no gateway", e.getMessage());
    }

    @Test
    void dataDeHojeVemDoClockInjetado() {
        service(FakePaymentGateway.approving(), JUNHO).processMonthlyBilling(cobranca("c1", "BLACKFRIDAY"));
        service(FakePaymentGateway.approving(), NOVEMBRO).processMonthlyBilling(cobranca("c1", "BLACKFRIDAY"));

        assertEquals(List.of(Money.of("100"), Money.of("70")),
                invoices.saved().stream().map(SavedInvoice::amount).toList());
    }

    @Test
    @SuppressWarnings("deprecation")
    void assinaturaAntigaDelegaParaANova() {
        List<String> logs = service(FakePaymentGateway.approving(), JUNHO)
                .processMonthlyBilling("c1", "PRO", 100.0, "2026-01-10", 0, null, false, "tok_123");

        assertEquals("Pagamento processado com sucesso: R$ 100.00", logs.get(0));
    }

    /** Os mesmos 22 cenários de caracterização, agora passando pelo serviço de verdade. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.saas.billing.PricingScenarios#all")
    void valorDaFaturaBateComOsCenariosDeCaracterizacao(PricingScenario s) {
        BillingCommand command = new BillingCommand("c1", "PRO", Money.of(s.basePrice()), s.signupDate(),
                s.overdueDays(), s.promoCode(), s.nonProfit(), "tok_123");

        service(FakePaymentGateway.approving(), s.today()).processMonthlyBilling(command);

        assertEquals(Money.of(s.expectedPrice()), invoices.saved().get(0).amount());
    }
}
