package com.saas.billing;

/** Porta de envio de avisos ao cliente. */
public interface NotificationService {

    void sendInvoiceProcessed(String email, Money amount);
}
