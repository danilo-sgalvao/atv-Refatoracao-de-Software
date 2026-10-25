package com.saas.billing;

/**
 * Falha de infraestrutura (banco, SMTP) traduzida para exceção não checada,
 * para que as portas não exponham SQLException/EmailException.
 * A mensagem é a da causa, para manter o texto do log "Erro fatal no processamento".
 */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
