package dev.caixadev.infrastructure.persistence.adapter;

import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.port.out.ApiKeyRepository;
import dev.caixadev.infrastructure.persistence.entity.ApiKeyEntity;
import dev.caixadev.infrastructure.persistence.repository.ApiKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur JPA des clés API. Les portées sont stockées en texte séparé par
 * des virgules ; la conversion liste/texte reste confinée ici.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpa;

    @Override
    public ApiKey save(ApiKey apiKey) {
        return toDomain(jpa.save(toEntity(apiKey)));
    }

    @Override
    public Optional<ApiKey> findById(UUID id) {
        return jpa.findById(id).map(ApiKeyRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ApiKey> findByPrefix(String keyPrefix) {
        return jpa.findByKeyPrefix(keyPrefix).map(ApiKeyRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByPrefix(String keyPrefix) {
        return jpa.existsByKeyPrefix(keyPrefix);
    }

    @Override
    public List<ApiKey> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream().map(ApiKeyRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public void markRevoked(UUID id, Instant revokedAt) {
        jpa.markRevoked(id, revokedAt);
    }

    @Override
    @Transactional
    public void markUsed(UUID id, Instant lastUsedAt) {
        jpa.markUsed(id, lastUsedAt);
    }

    private static ApiKeyEntity toEntity(ApiKey apiKey) {
        return ApiKeyEntity.builder()
                .id(apiKey.id())
                .name(apiKey.name())
                .keyPrefix(apiKey.keyPrefix())
                .keyHash(apiKey.keyHash())
                .scopes(String.join(",", apiKey.scopes()))
                .createdBy(apiKey.createdBy())
                .createdAt(apiKey.createdAt() != null ? apiKey.createdAt() : Instant.now())
                .expiresAt(apiKey.expiresAt())
                .lastUsedAt(apiKey.lastUsedAt())
                .revokedAt(apiKey.revokedAt())
                .build();
    }

    private static ApiKey toDomain(ApiKeyEntity entity) {
        List<String> scopes = Arrays.stream(entity.getScopes().split(","))
                .map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .toList();
        return new ApiKey(
                entity.getId(),
                entity.getName(),
                entity.getKeyPrefix(),
                entity.getKeyHash(),
                scopes,
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getLastUsedAt(),
                entity.getRevokedAt()
        );
    }
}
