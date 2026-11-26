package dev.caixadev.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyEntity {

    /** Identifiant attribué par l'application (UUID aléatoire). */
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_prefix", nullable = false, unique = true, length = 8)
    private String keyPrefix;

    /** Empreinte SHA-256 hexadécimale, colonne CHAR(64). */
    @Column(name = "key_hash", nullable = false, length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String keyHash;

    /** Portées séparées par des virgules. */
    @Column(nullable = false, columnDefinition = "text")
    private String scopes;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Override
    public String toString() {
        return "ApiKeyEntity[id=" + id + ", keyPrefix=" + keyPrefix + "]";
    }
}
