package dev.caixadev.domain.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(Long id) {
        super("Rôle introuvable : " + id);
    }
}

