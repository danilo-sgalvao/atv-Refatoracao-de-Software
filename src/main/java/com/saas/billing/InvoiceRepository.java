package com.saas.billing;

/** Porta de gravação das faturas geradas pela cobrança. */
public interface InvoiceRepository {

    enum Status { PAID, FAILED }

    void save(String customerId, Money amount, Status status);
}
