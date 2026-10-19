package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    /** Os mesmos 22 cenários que caracterizam o legado devem passar no calculador novo. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.saas.billing.PricingScenarios#all")
    void mantemOComportamentoDoLegado(PricingScenario s) {
        BillingCommand command = new BillingCommand(
                "cliente-teste", "PRO", Money.of(s.basePrice()), s.signupDate(),
                s.overdueDays(), s.promoCode(), s.nonProfit(), "tok_teste");

        Money price = calculator.calculate(command, s.today());

        assertEquals(Money.of(s.expectedPrice()), price);
    }

    @Test
    void resultadoSaiArredondadoParaCentavos() {
        // 100 * 0.70 = 70; atraso de 3 dias: 70 + 1.40 + 0.21 = 71.61
        BillingCommand command = new BillingCommand(
                "c1", "PRO", Money.of("100"), LocalDate.of(2026, 1, 10),
                3, null, true, "tok_1");

        Money price = calculator.calculate(command, LocalDate.of(2026, 6, 15));

        assertEquals("71.61", price.amount().toPlainString());
    }

    @Test
    void mesesAtivosIgnoramODiaDoMes() {
        assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 1)));
        assertEquals(13, PricingCalculator.monthsActive(LocalDate.of(2025, 5, 1), LocalDate.of(2026, 6, 15)));
    }
}
