package dev.caixadev.domain.exception;

import java.util.UUID;

public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(UUID id) {
        super("Clé API introuvable : " + id);
    }
}
