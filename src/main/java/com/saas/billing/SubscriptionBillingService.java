package com.saas.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final PaymentGateway paymentGateway;
    private final NotificationService notifications;
    private final Clock clock;

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

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        Money basePrice = command.basePrice();
        Money calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(new BigDecimal("0.15"))); // 15% após 2 anos
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(new BigDecimal("0.10"))); // 10% após 1 ano
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.times(new BigDecimal("0.70")); // 30% off para ONGs
        }

        if (command.hasPromoCode()) {
            String promoCode = command.promoCode();
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice.minus(Money.of("30.00"));
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice.times(new BigDecimal("0.90"));
            }
        }

        if (command.isOverdue()) {
            Money penalty = calculatedPrice.times(new BigDecimal("0.02"));
            Money dailyInterest = calculatedPrice.times(new BigDecimal("0.001")).times(command.overdueDays());
            calculatedPrice = calculatedPrice.plus(penalty).plus(dailyInterest);
        }

        calculatedPrice = calculatedPrice.atLeastZero().rounded();

        String customerId = command.customerId();

        try {
            Optional<Customer> found = customers.findById(customerId);
            if (found.isEmpty()) {
                return logs;
            }
            Customer customer = found.get();

            if (!customer.isActive()) {
                logs.add("Cliente inativo. Cobrança cancelada: " + customerId);
                return logs;
            }

            boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), calculatedPrice);
            if (paymentSuccess) {
                logs.add("Pagamento processado com sucesso: R$ " + calculatedPrice);
            } else {
                logs.add("Falha no pagamento do cartão para: " + customerId);
            }

            invoices.save(customerId, calculatedPrice,
                    paymentSuccess ? InvoiceRepository.Status.PAID : InvoiceRepository.Status.FAILED);

            notifications.sendInvoiceProcessed(customer.email(), calculatedPrice);
            logs.add("E-mail disparado para: " + customer.email());
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
