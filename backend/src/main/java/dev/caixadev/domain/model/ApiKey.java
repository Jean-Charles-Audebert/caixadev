package dev.caixadev.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Clé d'accès machine. Ne contient jamais la clé en clair : seulement son
 * préfixe public et l'empreinte SHA-256 de la clé complète.
 */
public record ApiKey(
        UUID id,
        String name,
        String keyPrefix,
        String keyHash,
        List<String> scopes,
        String createdBy,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt
) {

    public ApiKey {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** Utilisable : ni révoquée, ni expirée. */
    public boolean isActive(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    @Override
    public String toString() {
        // L'empreinte n'a rien à faire dans un journal.
        return "ApiKey[id=" + id + ", name=" + name + ", keyPrefix=" + keyPrefix + ", scopes=" + scopes + "]";
    }
}
