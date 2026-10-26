package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SubscriptionBillingService {

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentGateway paymentGateway;
    private final NotificationService notifications;
    private final Clock clock;
    private final PricingCalculator pricing = new PricingCalculator();

    public SubscriptionBillingService(CustomerRepository customers, InvoiceRepository invoices,
                                      PaymentGateway paymentGateway, NotificationService notifications,
                                      Clock clock) {
        this.customers = Objects.requireNonNull(customers, "customers");
        this.invoices = Objects.requireNonNull(invoices, "invoices");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway");
        this.notifications = Objects.requireNonNull(notifications, "notifications");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Construtor antigo, mantido para não quebrar quem fazia {@code new SubscriptionBillingService()}.
     * Monta as implementações reais com a configuração lida do ambiente.
     */
    @Deprecated
    public SubscriptionBillingService() {
        this(DatabaseConfig.fromEnvironment(), SmtpConfig.fromEnvironment());
    }

    private SubscriptionBillingService(DatabaseConfig db, SmtpConfig smtp) {
        this(new JdbcCustomerRepository(db), new JdbcInvoiceRepository(db),
                new StubPaymentGateway(), new CommonsEmailNotificationService(smtp),
                Clock.systemDefaultZone());
    }

    /**
     * Assinatura antiga, mantida para não quebrar quem já chama o serviço.
     * Apenas monta o {@link BillingCommand} e delega.
     */
    @Deprecated
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
                                             String signupDateStr, int overdueDays, String promoCode,
                                             boolean isNonProfit, String paymentCardToken) {
        return processMonthlyBilling(new BillingCommand(
                customerId, planType, Money.of(basePrice), LocalDate.parse(signupDateStr),
                overdueDays, promoCode, isNonProfit, paymentCardToken));
    }

    /**
     * Fachada da cobrança mensal: calcula o preço e, se o cliente existir,
     * cobra, grava a fatura e avisa. Cliente inexistente não gera nada, como no legado.
     */
    public List<String> processMonthlyBilling(BillingCommand command) {
        List<String> logs = new ArrayList<>();
        Money amount = pricing.calculate(command, LocalDate.now(clock));
        try {
            customers.findById(command.customerId())
                    .ifPresent(customer -> bill(customer, command.paymentCardToken(), amount, logs));
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }
        return logs;
    }

    private void bill(Customer customer, String cardToken, Money amount, List<String> logs) {
        if (!customer.isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + customer.id());
            return;
        }
        boolean paid = paymentGateway.charge(cardToken, amount);
        logs.add(paid
                ? "Pagamento processado com sucesso: R$ " + amount
                : "Falha no pagamento do cartão para: " + customer.id());
        invoices.save(customer.id(), amount, paid ? InvoiceRepository.Status.PAID : InvoiceRepository.Status.FAILED);
        notifications.sendInvoiceProcessed(customer.email(), amount);
        logs.add("E-mail disparado para: " + customer.email());
    }
}
