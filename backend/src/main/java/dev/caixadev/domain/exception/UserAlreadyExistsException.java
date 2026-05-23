package dev.caixadev.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("L'email est déjà utilisé : " + email);
    }
}

