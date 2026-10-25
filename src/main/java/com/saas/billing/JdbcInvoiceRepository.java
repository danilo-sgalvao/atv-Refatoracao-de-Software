package com.saas.billing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class JdbcInvoiceRepository implements InvoiceRepository {

    private static final String INSERT =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    private final DatabaseConfig config;

    public JdbcInvoiceRepository(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void save(String customerId, Money amount, Status status) {
        try (Connection conn = config.openConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT)) {
            stmt.setString(1, customerId);
            stmt.setBigDecimal(2, amount.rounded().amount());
            stmt.setString(3, status.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException(e);
        }
    }
}
