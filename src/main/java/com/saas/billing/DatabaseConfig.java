package com.saas.billing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/** Credenciais do banco, lidas do ambiente em vez de fixas no código. */
public record DatabaseConfig(String url, String user, String password) {

    public static final String URL_VAR = "BILLING_DB_URL";
    public static final String USER_VAR = "BILLING_DB_USER";
    public static final String PASSWORD_VAR = "BILLING_DB_PASSWORD";

    public DatabaseConfig {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
    }

    public static DatabaseConfig fromEnvironment() {
        return from(System.getenv());
    }

    static DatabaseConfig from(Map<String, String> env) {
        return new DatabaseConfig(
                Environment.required(env, URL_VAR),
                Environment.required(env, USER_VAR),
                Environment.required(env, PASSWORD_VAR));
    }

    Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=***]";
    }
}
