package com.saas.billing;

import java.util.ArrayList;
import java.util.List;

/** Fake de {@link PaymentGateway} com resposta fixa; registra cada cobrança feita. */
final class FakePaymentGateway implements PaymentGateway {

    record Charge(String cardToken, Money amount) {
    }

    private final boolean approves;
    private final List<Charge> charges = new ArrayList<>();

    private FakePaymentGateway(boolean approves) {
        this.approves = approves;
    }

    static FakePaymentGateway approving() {
        return new FakePaymentGateway(true);
    }

    static FakePaymentGateway declining() {
        return new FakePaymentGateway(false);
    }

    @Override
    public boolean charge(String cardToken, Money amount) {
        charges.add(new Charge(cardToken, amount));
        return approves;
    }

    List<Charge> charges() {
        return List.copyOf(charges);
    }
}
