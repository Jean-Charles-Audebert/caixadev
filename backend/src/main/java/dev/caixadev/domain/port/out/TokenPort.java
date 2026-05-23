package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.TokenClaims;
import dev.caixadev.domain.model.User;

public interface TokenPort {
    /** Génère un access token JWT signé. */
    String generateAccessToken(User user);

    /** Parse et valide un access token — lève InvalidTokenException si invalide. */
    TokenClaims parseAccessToken(String token);
}

