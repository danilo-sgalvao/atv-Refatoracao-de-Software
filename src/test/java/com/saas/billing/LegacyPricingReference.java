package com.saas.billing;

import java.time.LocalDate;

/**
 * Cópia FIEL da lógica de preço de SubscriptionBillingService.processMonthlyBilling
 * (linhas 27-57 do legado), com "today" recebido por parâmetro em vez de LocalDate.now().
 * Serve só para validar os cálculos feitos à mão em PricingScenarios.
 * Não faz parte do código de produção e pode ser removida no fim da refatoração.
 */
final class LegacyPricingReference {

    private LegacyPricingReference() {
    }

    static double price(double basePrice, LocalDate signupDate, LocalDate today,
                        int overdueDays, String promoCode, boolean isNonProfit) {

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());

        double calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice - (basePrice * 0.15);
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice - (basePrice * 0.10);
        }

        if (isNonProfit) {
            calculatedPrice = calculatedPrice * 0.70;
        }

        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice - 30.0;
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice * 0.90;
            }
        }

        if (overdueDays > 0) {
            double penalty = calculatedPrice * 0.02;
            double dailyInterest = (calculatedPrice * 0.001) * overdueDays;
            calculatedPrice = calculatedPrice + penalty + dailyInterest;
        }

        if (calculatedPrice < 0) {
            calculatedPrice = 0.0;
        }
        return calculatedPrice;
    }
}
