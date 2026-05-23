package dev.caixadev.domain.model;

/** Claims extraits d'un access token JWT (domaine pur, sans dépendance JJWT). */
public record TokenClaims(
        Long userId,
        String email,
        boolean isSuperAdmin
) {}

