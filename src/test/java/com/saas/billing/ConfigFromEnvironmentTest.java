package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigFromEnvironmentTest {

    private static final Map<String, String> DB_ENV = Map.of(
            DatabaseConfig.URL_VAR, "jdbc:postgresql://db:5432/saas_db",
            DatabaseConfig.USER_VAR, "billing",
            DatabaseConfig.PASSWORD_VAR, "s3cr3t");

    @Test
    void leCredenciaisDoBancoDoAmbiente() {
        assertEquals(new DatabaseConfig("jdbc:postgresql://db:5432/saas_db", "billing", "s3cr3t"),
                DatabaseConfig.from(DB_ENV));
    }

    @Test
    void faltaDeVariavelDoBancoFalhaCedoComONomeDela() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> DatabaseConfig.from(Map.of(DatabaseConfig.URL_VAR, "jdbc:x", DatabaseConfig.USER_VAR, "u")));
        assertTrue(e.getMessage().contains(DatabaseConfig.PASSWORD_VAR));
    }

    @Test
    void toStringNaoExpoeSenhas() {
        assertFalse(DatabaseConfig.from(DB_ENV).toString().contains("s3cr3t"));
        assertFalse(new SmtpConfig("smtp", 587, "a@b.com", "u", "s3cr3t").toString().contains("s3cr3t"));
    }

    @Test
    void smtpUsaPorta587ESemAutenticacaoPorPadrao() {
        SmtpConfig config = SmtpConfig.from(Map.of(
                SmtpConfig.HOST_VAR, "smtp.empresa.com",
                SmtpConfig.FROM_VAR, "billing@empresa.com"));

        assertEquals(587, config.port());
        assertFalse(config.hasAuthentication());
    }

    @Test
    void smtpComUsuarioExigeSenha() {
        assertThrows(IllegalStateException.class, () -> SmtpConfig.from(Map.of(
                SmtpConfig.HOST_VAR, "smtp.empresa.com",
                SmtpConfig.FROM_VAR, "billing@empresa.com",
                SmtpConfig.USER_VAR, "billing")));
    }
}
