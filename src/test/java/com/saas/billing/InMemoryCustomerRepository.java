package com.saas.billing;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fake em memória de {@link CustomerRepository}. */
final class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new HashMap<>();

    InMemoryCustomerRepository with(Customer customer) {
        customers.put(customer.id(), customer);
        return this;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
