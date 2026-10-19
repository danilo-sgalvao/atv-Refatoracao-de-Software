package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void operacoesNaoAlteramOValorOriginal() {
        Money cem = Money.of("100");

        Money resultado = cem.minus(Money.of("30"));

        assertEquals(Money.of("100"), cem);
        assertEquals(Money.of("70"), resultado);
        assertNotSame(cem, resultado);
    }

    @Test
    void ofDoubleUsaARepresentacaoDecimal() {
        assertEquals(Money.of("49.90"), Money.of(49.9));
    }

    @Test
    void naoAcumulaErroDeDouble() {
        // em double: 0.1 + 0.2 = 0.30000000000000004
        assertEquals(Money.of("0.30"), Money.of("0.1").plus(Money.of("0.2")));
    }

    @Test
    void multiplicaPorFatorEPorInteiro() {
        assertEquals(Money.of("90"), Money.of("100").times(new BigDecimal("0.90")));
        assertEquals(Money.of("1.00"), Money.of("0.10").times(10));
    }

    @Test
    void arredondaParaCentavosComHalfUp() {
        assertEquals(new BigDecimal("30.39"), Money.of("30.385").rounded().amount());
        assertEquals(new BigDecimal("30.38"), Money.of("30.3849").rounded().amount());
    }

    @Test
    void pisoEmZero() {
        assertTrue(Money.of("-10.25").isNegative());
        assertSame(Money.ZERO, Money.of("-10.25").atLeastZero());
        assertEquals(Money.of("5"), Money.of("5").atLeastZero());
    }

    @Test
    void igualdadeIgnoraEscala() {
        assertEquals(Money.of("10.0"), Money.of("10.00"));
        assertEquals(Money.of("10.0").hashCode(), Money.of("10.00").hashCode());
    }

    @Test
    void toStringMostraCentavos() {
        assertEquals("100.00", Money.of("100").toString());
    }

    @Test
    void rejeitaValorNulo() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }
}
