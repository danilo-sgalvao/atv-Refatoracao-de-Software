package com.saas.billing;

import java.util.Map;
import java.util.Objects;

/**
 * Configuração do SMTP lida do ambiente. Usuário e senha são opcionais
 * (o legado não autenticava); se o usuário vier, a senha é obrigatória.
 */
public record SmtpConfig(String host, int port, String from, String user, String password) {

    public static final String HOST_VAR = "BILLING_SMTP_HOST";
    public static final String PORT_VAR = "BILLING_SMTP_PORT";
    public static final String FROM_VAR = "BILLING_MAIL_FROM";
    public static final String USER_VAR = "BILLING_SMTP_USER";
    public static final String PASSWORD_VAR = "BILLING_SMTP_PASSWORD";

    static final int DEFAULT_PORT = 587;

    public SmtpConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(from, "from");
    }

    public static SmtpConfig fromEnvironment() {
        return from(System.getenv());
    }

    static SmtpConfig from(Map<String, String> env) {
        String user = Environment.optional(env, USER_VAR, null);
        String password = user == null ? null : Environment.required(env, PASSWORD_VAR);
        return new SmtpConfig(
                Environment.required(env, HOST_VAR),
                Integer.parseInt(Environment.optional(env, PORT_VAR, String.valueOf(DEFAULT_PORT))),
                Environment.required(env, FROM_VAR),
                user,
                password);
    }

    public boolean hasAuthentication() {
        return user != null;
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "SmtpConfig[host=" + host + ", port=" + port + ", from=" + from
                + ", user=" + user + ", password=***]";
    }
}
