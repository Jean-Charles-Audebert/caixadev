package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.User;

import java.time.LocalDateTime;

/**
 * {@code superAdmin} sert uniquement à l'affichage côté frontend (liens
 * d'administration) : l'autorisation reste vérifiée côté serveur.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName,
        boolean superAdmin,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.isSuperAdmin(), user.createdAt());
    }
}
