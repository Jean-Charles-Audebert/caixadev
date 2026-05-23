package dev.caixadev.domain.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(Long id) {
        super("Projet introuvable : " + id);
    }

    public ProjectNotFoundException(String key) {
        super("Projet introuvable : " + key);
    }
}

