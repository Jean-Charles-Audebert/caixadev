package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.User;

/** Données de session retournées après authentification — jamais les tokens (cookies HttpOnly). */
public record AuthResponse(
        Long id,
        String email,
        String displayName
) {
    public static AuthResponse from(User user) {
        return new AuthResponse(user.id(), user.email(), user.displayName());
    }
}

