package com.saas.billing;

import java.util.Optional;

/** Porta de leitura de clientes. */
public interface CustomerRepository {

    Optional<Customer> findById(String customerId);
}
