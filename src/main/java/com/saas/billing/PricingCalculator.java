package com.saas.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/**
 * Regras de precificação da assinatura mensal, como domínio puro:
 * sem banco, rede, e-mail ou relógio do sistema ("today" chega por parâmetro).
 *
 * A ordem das regras é a do sistema legado e faz parte do comportamento:
 * tempo de casa → ONG → cupom → multa e juros por atraso → piso em zero.
 * O piso vem depois do atraso de propósito: um preço negativo continua
 * negativo após multa e juros, e só então vira zero.
 */
public final class PricingCalculator {

    static final int LOYALTY_TIER_1_MONTHS = 12;
    static final int LOYALTY_TIER_2_MONTHS = 24;
    static final BigDecimal LOYALTY_TIER_1_DISCOUNT = new BigDecimal("0.10");
    static final BigDecimal LOYALTY_TIER_2_DISCOUNT = new BigDecimal("0.15");

    static final BigDecimal NON_PROFIT_FACTOR = new BigDecimal("0.70");

    static final String BLACK_FRIDAY_CODE = "BLACKFRIDAY";
    static final Month BLACK_FRIDAY_MONTH = Month.NOVEMBER;
    static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");

    static final String WELCOME_CODE = "WELCOME10";
    static final int WELCOME_MAX_MONTHS = 1;
    static final BigDecimal WELCOME_FACTOR = new BigDecimal("0.90");

    static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    static final BigDecimal LATE_DAILY_INTEREST_RATE = new BigDecimal("0.001");

    /** Preço final da fatura, já com piso em zero e arredondado para centavos. */
    public Money calculate(BillingCommand command, LocalDate today) {
        int monthsActive = monthsActive(command.signupDate(), today);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command, monthsActive, today);
        price = applyLateFees(price, command);

        return price.atLeastZero().rounded();
    }

    /** Meses de calendário entre a adesão e hoje; o dia do mês é ignorado, como no legado. */
    static int monthsActive(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    /** O desconto incide sobre o preço base (é a primeira regra, então base = preço atual). */
    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > LOYALTY_TIER_2_MONTHS) {
            return basePrice.minus(basePrice.times(LOYALTY_TIER_2_DISCOUNT));
        }
        if (monthsActive > LOYALTY_TIER_1_MONTHS) {
            return basePrice.minus(basePrice.times(LOYALTY_TIER_1_DISCOUNT));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.times(NON_PROFIT_FACTOR) : price;
    }

    private Money applyPromoCode(Money price, BillingCommand command, int monthsActive, LocalDate today) {
        if (!command.hasPromoCode()) {
            return price;
        }
        String code = command.promoCode();
        if (code.equalsIgnoreCase(BLACK_FRIDAY_CODE) && today.getMonth() == BLACK_FRIDAY_MONTH) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (code.equalsIgnoreCase(WELCOME_CODE) && monthsActive <= WELCOME_MAX_MONTHS) {
            return price.times(WELCOME_FACTOR);
        }
        return price;
    }

    /** Multa de 2% + juros de 0,1% ao dia, ambos sobre o preço já descontado. */
    private Money applyLateFees(Money price, BillingCommand command) {
        if (!command.isOverdue()) {
            return price;
        }
        Money penalty = price.times(LATE_PENALTY_RATE);
        Money interest = price.times(LATE_DAILY_INTEREST_RATE).times(command.overdueDays());
        return price.plus(penalty).plus(interest);
    }
}
