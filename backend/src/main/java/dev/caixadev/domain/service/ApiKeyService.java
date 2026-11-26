package dev.caixadev.domain.service;

import dev.caixadev.domain.exception.ApiKeyNotFoundException;
import dev.caixadev.domain.exception.InvalidApiKeyRequestException;
import dev.caixadev.domain.model.ApiKey;
import dev.caixadev.domain.model.ApiKeyCreation;
import dev.caixadev.domain.model.ApiKeyScopes;
import dev.caixadev.domain.model.ApiKeySecret;
import dev.caixadev.domain.model.ApiKeyVerification;
import dev.caixadev.domain.model.ApiKeyVerification.Status;
import dev.caixadev.domain.port.in.ApiKeyUseCase;
import dev.caixadev.domain.port.out.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService implements ApiKeyUseCase {

    /** Évite une écriture en base à chaque appel : la date n'est rafraîchie qu'au-delà de ce délai. */
    static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PREFIX_ATTEMPTS = 5;

    private final ApiKeyRepository apiKeyRepository;

    @Override
    @Transactional
    public ApiKeyCreation create(String name, List<String> scopes, Instant expiresAt, String createdBy) {
        Instant now = Instant.now();
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty() || trimmedName.length() > MAX_NAME_LENGTH) {
            throw new InvalidApiKeyRequestException("Le nom de la clé est obligatoire (100 caractères au plus)");
        }
        List<String> normalizedScopes = normalizeScopes(scopes);
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new InvalidApiKeyRequestException("La date d'expiration doit être dans le futur");
        }

        ApiKeySecret.Generated generated = generateUniquePrefix();
        ApiKey saved = apiKeyRepository.save(new ApiKey(
                UUID.randomUUID(),
                trimmedName,
                generated.keyPrefix(),
                ApiKeySecret.hash(generated.rawKey()),
                normalizedScopes,
                createdBy,
                now,
                expiresAt,
                null,
                null
        ));
        return new ApiKeyCreation(saved, generated.rawKey());
    }

    @Override
    public List<ApiKey> list() {
        return apiKeyRepository.findAll();
    }

    @Override
    @Transactional
    public void revoke(UUID id) {
        ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
        if (!apiKey.isRevoked()) {
            apiKeyRepository.markRevoked(id, Instant.now());
        }
    }

    @Override
    @Transactional
    public ApiKeyVerification authenticate(String rawKey) {
        Optional<String> prefix = ApiKeySecret.parsePrefix(rawKey);
        if (prefix.isEmpty()) {
            return ApiKeyVerification.failure(Status.MALFORMED, null);
        }
        Optional<ApiKey> found = apiKeyRepository.findByPrefix(prefix.get());
        if (found.isEmpty()) {
            return ApiKeyVerification.failure(Status.UNKNOWN, prefix.get());
        }
        ApiKey apiKey = found.get();
        if (!ApiKeySecret.hashMatches(apiKey.keyHash(), rawKey)) {
            return ApiKeyVerification.failure(Status.INVALID_SECRET, prefix.get());
        }
        Instant now = Instant.now();
        if (apiKey.isRevoked()) {
            return ApiKeyVerification.failure(Status.REVOKED, prefix.get());
        }
        if (apiKey.isExpired(now)) {
            return ApiKeyVerification.failure(Status.EXPIRED, prefix.get());
        }
        if (apiKey.lastUsedAt() == null || apiKey.lastUsedAt().plus(LAST_USED_RESOLUTION).isBefore(now)) {
            apiKeyRepository.markUsed(apiKey.id(), now);
        }
        return ApiKeyVerification.valid(apiKey);
    }

    private static List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new InvalidApiKeyRequestException("Au moins une portée est requise");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String scope : scopes) {
            String value = scope == null ? "" : scope.trim();
            if (!ApiKeyScopes.KNOWN.contains(value)) {
                throw new InvalidApiKeyRequestException("Portée inconnue");
            }
            normalized.add(value);
        }
        return List.copyOf(normalized);
    }

    private ApiKeySecret.Generated generateUniquePrefix() {
        for (int attempt = 0; attempt < MAX_PREFIX_ATTEMPTS; attempt++) {
            ApiKeySecret.Generated generated = ApiKeySecret.generate();
            if (!apiKeyRepository.existsByPrefix(generated.keyPrefix())) {
                return generated;
            }
        }
        throw new IllegalStateException("Impossible de générer un préfixe de clé unique");
    }
}
