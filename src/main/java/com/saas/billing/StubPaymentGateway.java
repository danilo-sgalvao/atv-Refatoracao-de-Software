package com.saas.billing;

/**
 * Gateway falso herdado do legado: aprova qualquer token que comece com "tok_".
 * Não cobra nada de verdade; deve ser trocado por uma integração real.
 */
public class StubPaymentGateway implements PaymentGateway {

    private static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
    }
}
