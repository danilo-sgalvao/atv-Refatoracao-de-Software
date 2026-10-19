package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingCommandTest {

    private static BillingCommand comPromoEAtraso(String promoCode, int overdueDays) {
        return new BillingCommand("c1", "PRO", Money.of("100"), LocalDate.of(2026, 1, 10),
                overdueDays, promoCode, false, "tok_123");
    }

    @Test
    void promoNulaOuEmBrancoNaoConta() {
        assertFalse(comPromoEAtraso(null, 0).hasPromoCode());
        assertFalse(comPromoEAtraso("   ", 0).hasPromoCode());
        assertTrue(comPromoEAtraso("WELCOME10", 0).hasPromoCode());
    }

    @Test
    void soEstaAtrasadoComDiasPositivos() {
        assertFalse(comPromoEAtraso(null, 0).isOverdue());
        assertFalse(comPromoEAtraso(null, -3).isOverdue());
        assertTrue(comPromoEAtraso(null, 1).isOverdue());
    }

    @Test
    void camposObrigatorios() {
        LocalDate data = LocalDate.of(2026, 1, 10);
        assertThrows(NullPointerException.class,
                () -> new BillingCommand(null, "PRO", Money.of("100"), data, 0, null, false, "tok_1"));
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("c1", "PRO", null, data, 0, null, false, "tok_1"));
        assertThrows(NullPointerException.class,
                () -> new BillingCommand("c1", "PRO", Money.of("100"), null, 0, null, false, "tok_1"));
    }
}
