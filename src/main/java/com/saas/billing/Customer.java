package com.saas.billing;

import java.util.Objects;

/** Dados do cliente que a cobrança precisa: para onde mandar a fatura e se ele está ativo. */
public record Customer(String id, String email, String status) {

    public Customer {
        Objects.requireNonNull(id, "id");
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
