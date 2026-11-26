package dev.caixadev.application.dto.response;

import dev.caixadev.domain.model.ApiKey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Vue d'administration d'une clé API : ni la clé, ni son empreinte. */
public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        List<String> scopes,
        String createdBy,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant revokedAt,
        String status
) {
    public static ApiKeyResponse from(ApiKey apiKey) {
        Instant now = Instant.now();
        String status = apiKey.isRevoked() ? "REVOKED" : apiKey.isExpired(now) ? "EXPIRED" : "ACTIVE";
        return new ApiKeyResponse(
                apiKey.id(),
                apiKey.name(),
                apiKey.keyPrefix(),
                apiKey.scopes(),
                apiKey.createdBy(),
                apiKey.createdAt(),
                apiKey.expiresAt(),
                apiKey.lastUsedAt(),
                apiKey.revokedAt(),
                status
        );
    }
}
