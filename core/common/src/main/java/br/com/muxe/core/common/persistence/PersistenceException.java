package br.com.muxe.core.common.persistence;

public final class PersistenceException extends RuntimeException {

    public PersistenceException(String operation, Throwable cause) {
        super("Persistence operation '" + operation + "' failed: " + cause.getMessage(), cause);
    }
}
