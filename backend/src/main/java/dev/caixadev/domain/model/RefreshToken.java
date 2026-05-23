package dev.caixadev.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

public record RefreshToken(
        Long id,
        Long userId,
        String tokenHash,
        LocalDateTime expiresAt,
        boolean revoked,
        LocalDateTime createdAt
) {

    /** Crée un nouveau RefreshToken à partir du token brut. */
    public static RefreshToken create(Long userId, String rawToken, int expirationDays) {
        return new RefreshToken(
                null,
                userId,
                hash(rawToken),
                LocalDateTime.now().plusDays(expirationDays),
                false,
                LocalDateTime.now()
        );
    }

    /** Vérifie si le token est encore valide (non révoqué, non expiré). */
    public boolean isValid() {
        return !revoked && LocalDateTime.now().isBefore(expiresAt);
    }

    /** Hash SHA-256 d'un token brut. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}

