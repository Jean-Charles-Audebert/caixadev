package dev.caixadev.infrastructure.persistence.repository;

import dev.caixadev.infrastructure.persistence.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByKeyPrefix(String keyPrefix);

    boolean existsByKeyPrefix(String keyPrefix);

    List<ApiKeyEntity> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE ApiKeyEntity k SET k.revokedAt = :revokedAt WHERE k.id = :id AND k.revokedAt IS NULL")
    void markRevoked(UUID id, Instant revokedAt);

    @Modifying
    @Query("UPDATE ApiKeyEntity k SET k.lastUsedAt = :lastUsedAt WHERE k.id = :id")
    void markUsed(UUID id, Instant lastUsedAt);
}
