package com.saas.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável para valores monetários (em reais).
 *
 * As operações são feitas em BigDecimal sem arredondar, para não acumular erro
 * entre uma regra de preço e outra. O arredondamento para centavos acontece em um
 * único lugar: {@link #rounded()}, usado na hora de cobrar, persistir ou exibir.
 */
public record Money(BigDecimal amount) {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    /** Usa a representação decimal do double (49.9 vira 49.9, e não 49.89999...). */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public Money times(int factor) {
        return times(BigDecimal.valueOf(factor));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /** Piso em zero: valores negativos viram {@link #ZERO}. */
    public Money atLeastZero() {
        return isNegative() ? ZERO : this;
    }

    /** Valor arredondado para centavos. */
    public Money rounded() {
        return new Money(amount.setScale(SCALE, ROUNDING));
    }

    /** Igualdade por valor numérico: 10.0 e 10.00 são o mesmo dinheiro. */
    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return rounded().amount.toPlainString();
    }
}
