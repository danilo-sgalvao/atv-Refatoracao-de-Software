package com.saas.billing;

import java.util.ArrayList;
import java.util.List;

/** Fake em memória de {@link InvoiceRepository}: guarda as faturas na ordem em que foram salvas. */
final class InMemoryInvoiceRepository implements InvoiceRepository {

    record SavedInvoice(String customerId, Money amount, Status status) {
    }

    private final List<SavedInvoice> saved = new ArrayList<>();

    @Override
    public void save(String customerId, Money amount, Status status) {
        saved.add(new SavedInvoice(customerId, amount, status));
    }

    List<SavedInvoice> saved() {
        return List.copyOf(saved);
    }
}
