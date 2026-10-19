package com.saas.billing;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Objeto de parâmetros (Introduce Parameter Object) da cobrança mensal.
 * Substitui os 8 parâmetros soltos de processMonthlyBilling.
 *
 * planType não é usado por nenhuma regra hoje; foi mantido para não mudar
 * o contrato de quem chama o serviço.
 */
public record BillingCommand(
        String customerId,
        String planType,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(basePrice, "basePrice");
        Objects.requireNonNull(signupDate, "signupDate");
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }
}
