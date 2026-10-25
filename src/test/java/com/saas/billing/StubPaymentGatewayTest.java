package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StubPaymentGatewayTest {

    private final StubPaymentGateway gateway = new StubPaymentGateway();

    @Test
    void aprovaTokenComPrefixoTok() {
        assertTrue(gateway.charge("tok_123", Money.of("10")));
    }

    @Test
    void recusaTokenNuloOuSemPrefixo() {
        assertFalse(gateway.charge(null, Money.of("10")));
        assertFalse(gateway.charge("card_123", Money.of("10")));
        assertFalse(gateway.charge("TOK_123", Money.of("10")));
    }
}
