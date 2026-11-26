package dev.caixadev.domain.exception;

/** Demande de création de clé API invalide (portée inconnue, expiration passée…). */
public class InvalidApiKeyRequestException extends RuntimeException {
    public InvalidApiKeyRequestException(String message) {
        super(message);
    }
}
