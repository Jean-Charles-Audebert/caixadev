package dev.caixadev.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Utilisateur introuvable : " + id);
    }

    public UserNotFoundException(String email) {
        super("Utilisateur introuvable : " + email);
    }
}

