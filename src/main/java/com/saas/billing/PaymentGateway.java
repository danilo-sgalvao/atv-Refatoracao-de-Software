package com.saas.billing;

/** Porta de cobrança no cartão. */
public interface PaymentGateway {

    /** @return true se o pagamento foi aprovado. */
    boolean charge(String cardToken, Money amount);
}
