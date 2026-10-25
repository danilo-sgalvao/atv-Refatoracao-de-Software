package com.saas.billing;

import java.util.Objects;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class CommonsEmailNotificationService implements NotificationService {

    private static final String SUBJECT = "Sua Fatura Mensal";

    private final SmtpConfig config;

    public CommonsEmailNotificationService(SmtpConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void sendInvoiceProcessed(String email, Money amount) {
        try {
            SimpleEmail message = new SimpleEmail();
            message.setHostName(config.host());
            message.setSmtpPort(config.port());
            if (config.hasAuthentication()) {
                message.setAuthentication(config.user(), config.password());
            }
            message.setFrom(config.from());
            message.setSubject(SUBJECT);
            message.setMsg("Olá! Sua fatura foi processada no valor de R$ " + amount);
            message.addTo(email);
            // message.send(); // desligado no legado; comportamento mantido nesta refatoração
        } catch (EmailException e) {
            throw new InfrastructureException(e);
        }
    }
}
