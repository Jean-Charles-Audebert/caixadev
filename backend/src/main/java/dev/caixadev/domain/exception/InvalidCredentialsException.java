package dev.caixadev.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Identifiants invalides");
    }
}

