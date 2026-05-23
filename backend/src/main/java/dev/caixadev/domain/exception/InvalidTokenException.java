package dev.caixadev.domain.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token invalide ou expiré");
    }
}

