package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LegacyPricingCharacterizationTest {

    private static final double TOLERANCIA = 0.005; // meio centavo

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.saas.billing.PricingScenarios#all")
    void precoDoLegadoBateComOEsperadoCalculadoAMao(PricingScenario s) {
        double atual = LegacyPricingReference.price(
                s.basePrice(), s.signupDate(), s.today(),
                s.overdueDays(), s.promoCode(), s.nonProfit());

        assertEquals(s.expectedPrice(), atual, TOLERANCIA);
    }
}
