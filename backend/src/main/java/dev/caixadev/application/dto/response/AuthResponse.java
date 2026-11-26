package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.User;

/**
 * Données de session retournées après authentification — jamais les tokens (cookies HttpOnly).
 * {@code superAdmin} ne sert qu'à l'affichage côté frontend.
 */
public record AuthResponse(
        Long id,
        String email,
        String displayName,
        boolean superAdmin
) {
    public static AuthResponse from(User user) {
        return new AuthResponse(user.id(), user.email(), user.displayName(), user.isSuperAdmin());
    }
}
