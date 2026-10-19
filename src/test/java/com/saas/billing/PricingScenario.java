package com.saas.billing;

import java.time.LocalDate;

/**
 * Cenário de precificação usando apenas tipos primitivos/JDK, de propósito:
 * assim ele independe das classes novas (Money, BillingCommand, PricingCalculator)
 * e pode ser reaproveitado depois da refatoração.
 * O valor esperado foi calculado à mão a partir das regras do legado.
 */
public record PricingScenario(
        String name,
        double basePrice,
        LocalDate signupDate,
        LocalDate today,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        double expectedPrice) {

    @Override
    public String toString() {
        return name;
    }
}
