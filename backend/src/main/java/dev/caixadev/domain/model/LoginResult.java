package dev.caixadev.domain.model;

/** Résultat d'une authentification réussie : données user + tokens bruts. */
public record LoginResult(
        User user,
        String accessToken,
        String rawRefreshToken
) {}

