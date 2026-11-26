package dev.caixadev.domain.port.out;

import dev.caixadev.domain.model.ApiKey;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {
    ApiKey save(ApiKey apiKey);

    Optional<ApiKey> findById(UUID id);

    Optional<ApiKey> findByPrefix(String keyPrefix);

    boolean existsByPrefix(String keyPrefix);

    /** Toutes les clés, les plus récentes d'abord. */
    List<ApiKey> findAll();

    void markRevoked(UUID id, Instant revokedAt);

    void markUsed(UUID id, Instant lastUsedAt);
}
